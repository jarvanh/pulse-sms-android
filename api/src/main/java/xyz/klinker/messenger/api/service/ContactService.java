package xyz.klinker.messenger.api.service;

import retrofit2.Call;
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
    Call<Void> add(@Body AddContactRequest request);

    @POST("contacts/update/{phone_number}")
    Call<Void> update(@Path("phone_number") String phoneNumber, @Query("account_id") String accountId,
                  @Body UpdateContactRequest request);

    @POST("contacts/remove/{phone_number}")
    Call<Void> remove(@Path("phone_number") String phoneNumber, @Query("account_id") String accountId);

    @POST("contacts/clear")
    Call<Void> clear(@Query("account_id") String accountId);

    @GET("contacts")
    Call<ContactBody[]> list(@Query("account_id") String accountId);
}
