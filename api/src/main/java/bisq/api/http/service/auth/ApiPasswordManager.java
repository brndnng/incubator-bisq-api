/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.api.http.service.auth;

import bisq.api.http.exceptions.UnauthorizedException;

import bisq.core.app.BisqEnvironment;

import bisq.common.crypto.Hash;

import com.google.inject.Inject;

import java.security.SecureRandom;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;


@Slf4j
public class ApiPasswordManager {

    private static final String SEPARATOR = ":";

    private final Path passwordFilePath;
    private final SecureRandom secureRandom;
    private boolean passwordSet;
    private String salt;
    private byte[] passwordDigest;

    @Inject
    public ApiPasswordManager(BisqEnvironment bisqEnvironment) {
        String appDataDir = bisqEnvironment.getAppDataDir();
        this.passwordFilePath = Paths.get(appDataDir).resolve("apipasswd");
        this.secureRandom = new SecureRandom();
        readPasswordFromFile();
    }

    public boolean isPasswordSet() {
        return passwordSet;
    }

    public void authenticate(String password) {
        final boolean passwordValid = isPasswordValid(password);
        if (!passwordValid)
            throw new UnauthorizedException();
    }

    @Nullable
    public void changePassword(@Nullable String oldPassword, @Nullable String newPassword) {
        if (passwordSet && (!isPasswordValid(oldPassword))) throw new UnauthorizedException();
        if (newPassword != null && newPassword.length() > 0) {
            salt = "" + secureRandom.nextLong();
            passwordDigest = getBytesForSaltedPassword(newPassword);
            passwordSet = true;
            writePasswordFile();
        } else {
            passwordSet = false;
            removePasswordFile();
        }
    }

    private void removePasswordFile() {
        try {
            Files.delete(passwordFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to remove password file: " + passwordFilePath, e);
        }
    }

    private boolean isPasswordValid(String password) {
        if (null == password) {
            return false;
        }
        final byte[] sha256Hash = getBytesForSaltedPassword(password);
        return Arrays.equals(sha256Hash, passwordDigest);
    }

    private byte[] getBytesForSaltedPassword(String password) {
        final StringBuilder stringBuilder = new StringBuilder(password);
        if (salt != null) {
            stringBuilder.append(salt);
        }
        return Hash.getSha256Hash(stringBuilder.toString());
    }

    private void writePasswordFile() {
        final String line = salt + SEPARATOR + new String(Base64.getEncoder().encode(passwordDigest));
        try {
            Files.write(passwordFilePath, Collections.singleton(line));
        } catch (IOException e) {
            throw new RuntimeException("Unable to write password file: " + passwordFilePath, e);
        }
    }

    private void readPasswordFromFile() {
        passwordSet = false;
        if (!Files.exists(this.passwordFilePath)) {
            return;
        }
        try {
            final List<String> lines = Files.readAllLines(this.passwordFilePath);
            final int linesCount = lines.size();
            if (linesCount != 1) {
                log.warn("API password file is corrupt. Expected to have 1 line, found {}", linesCount);
                return;
            }
            final String line = lines.get(0);
            final String[] segments = line.split(SEPARATOR);
            if (segments.length != 2) {
                log.warn("API password file is corrupt. Expected 2 segments, found {}", segments.length);
                return;
            }
            passwordSet = true;
            this.salt = segments[0];
            this.passwordDigest = Base64.getDecoder().decode(segments[1]);

        } catch (IOException e) {
            throw new RuntimeException("Unable to read api password file", e);
        }
    }
}
