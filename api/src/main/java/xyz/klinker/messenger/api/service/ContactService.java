package xyz.klinker.messenger.api.service;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import xyz.klinker.messenger.api.entity.AddContactRequest;
import xyz.klinker.messenger.api.entity.AddConversationRequest;
import xyz.klinker.messenger.api.entity.ContactBody;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.UpdateContactRequest;
import xyz.klinker.messenger.api.entity.UpdateConversationRequest;

public interface ContactService {

    @POST("contacts/add")
    Object add(@Body AddContactRequest request);

    @POST("contacts/update/{phone_number}")
    Object update(@Path("phone_number") String phoneNumber, @Query("account_id") String accountId,
                  @Body UpdateContactRequest request);

    @POST("contacts/remove/{phone_number}")
    Object remove(@Path("phone_number") String phoneNumber, @Query("account_id") String accountId);

    @GET("contacts")
    ContactBody[] list(@Query("account_id") String accountId);
}
