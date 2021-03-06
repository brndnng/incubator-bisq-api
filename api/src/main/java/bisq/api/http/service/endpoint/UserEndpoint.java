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

package bisq.api.http.service.endpoint;

import bisq.api.http.model.ChangePassword;
import bisq.api.http.service.auth.ApiPasswordManager;

import bisq.common.UserThread;

import javax.inject.Inject;



import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Tag(name = "user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserEndpoint {

    private final ApiPasswordManager apiPasswordManager;

    @Inject
    public UserEndpoint(ApiPasswordManager apiPasswordManager) {
        this.apiPasswordManager = apiPasswordManager;
    }

    @Operation(summary = "Change password")
    @POST
    @Path("/password")
    public void changePassword(@Suspended AsyncResponse asyncResponse, @Valid ChangePassword data) {
        UserThread.execute(() -> {
            try {
                apiPasswordManager.changePassword(data.oldPassword, data.newPassword);
                asyncResponse.resume(Response.noContent().build());
            } catch (Throwable e) {
                asyncResponse.resume(e);
            }
        });
    }
}
