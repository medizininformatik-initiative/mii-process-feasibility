package de.medizininformatik_initiative.process.feasibility.client.store;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import org.joda.time.DateTime;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.util.Base64;
import java.util.Optional;

final class OAuthInterceptor implements IClientInterceptor {

    private static final String HEADER_PROXY_AUTHORIZATION = "Proxy-Authorization";
    private static final int TOKEN_EXPIRY_THRESHOLD = 10000;
    private HTTPRequest tokenRequest;
    private AccessToken token;
    private DateTime tokenExpiry;

    public OAuthInterceptor(String oauthClientId, String oauthClientSecret, String oauthTokenUrl,
            Optional<String> proxyHost, Optional<Integer> proxyPort, Optional<String> proxyUsername,
            Optional<String> proxyPassword) {
        super();
        ClientSecretBasic clientAuth = new ClientSecretBasic(new ClientID(oauthClientId),
                new Secret(oauthClientSecret));
        HTTPRequest request = new TokenRequest(URI.create(oauthTokenUrl), clientAuth, new ClientCredentialsGrant())
                .toHTTPRequest();

        if (proxyHost.isPresent() && proxyPort.isPresent()) {
            Proxy proxy = new Proxy(Type.HTTP,
                    InetSocketAddress.createUnresolved(proxyHost.get(), proxyPort.get()));
            request.setProxy(proxy);

            if (proxyUsername.isPresent() && proxyPassword.isPresent()) {
                request.setHeader(HEADER_PROXY_AUTHORIZATION,
                        generateBasicAuthHeader(proxyUsername.get(), proxyPassword.get()));
            }
        }
        tokenRequest = request;
    }

    private String generateBasicAuthHeader(String username, String password) {
        return Constants.HEADER_AUTHORIZATION_VALPREFIX_BASIC
                + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(Constants.CHARSET_US_ASCII));
    }

    public String getToken() {
        if (token == null || tokenExpiry == null || tokenExpiry.isBefore(DateTime.now().plus(TOKEN_EXPIRY_THRESHOLD))) {
            try {
                TokenResponse response = TokenResponse.parse(tokenRequest.send());
                if (!response.indicatesSuccess()) {
                    TokenErrorResponse errorResponse = response.toErrorResponse();
                    throw new OAuth2ClientException(errorResponse.getErrorObject().getCode() + " - "
                            + errorResponse.getErrorObject().getDescription());
                }
                AccessTokenResponse successResponse = response.toSuccessResponse();

                token = successResponse.getTokens().getAccessToken();
                tokenExpiry = DateTime.now().plus(token.getLifetime() * 1000);
            } catch (ParseException | IOException e) {
                throw new OAuth2ClientException("OAuth2 access token tokenRequest failed", e);
            }
        }
        return token.getValue();
    }

    @Override
    public void interceptRequest(IHttpRequest theRequest) {
        theRequest.addHeader(Constants.HEADER_AUTHORIZATION,
                Constants.HEADER_AUTHORIZATION_VALPREFIX_BEARER + getToken());
    }

    @Override
    public void interceptResponse(IHttpResponse theResponse) throws IOException {
    }
}
