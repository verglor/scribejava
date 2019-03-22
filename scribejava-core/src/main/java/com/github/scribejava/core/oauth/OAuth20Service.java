package com.github.scribejava.core.oauth;

import java.io.IOException;
import java.util.concurrent.Future;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2Authorization;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.pkce.AuthorizationUrlWithPKCE;
import com.github.scribejava.core.pkce.PKCE;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import com.github.scribejava.core.revoke.TokenTypeHint;

public class OAuth20Service extends OAuthService {

    private static final String VERSION = "2.0";
    private final DefaultApi20 api;
    private final String responseType;
    private final String defaultScope;

    public OAuth20Service(DefaultApi20 api, String apiKey, String apiSecret, String callback, String defaultScope,
            String responseType, String userAgent, HttpClientConfig httpClientConfig, HttpClient httpClient) {
        super(apiKey, apiSecret, callback, userAgent, httpClientConfig, httpClient);
        this.responseType = responseType;
        this.api = api;
        this.defaultScope = defaultScope;
    }

    //protected to facilitate mocking
    protected OAuth2AccessToken sendAccessTokenRequestSync(OAuthRequest request)
            throws IOException, InterruptedException, ExecutionException {
        return api.getAccessTokenExtractor().extract(execute(request));
    }

    //protected to facilitate mocking
    protected Future<OAuth2AccessToken> sendAccessTokenRequestAsync(OAuthRequest request) {
        return sendAccessTokenRequestAsync(request, null);
    }

    //protected to facilitate mocking
    protected Future<OAuth2AccessToken> sendAccessTokenRequestAsync(OAuthRequest request,
            OAuthAsyncRequestCallback<OAuth2AccessToken> callback) {

        return execute(request, callback, new OAuthRequest.ResponseConverter<OAuth2AccessToken>() {
            @Override
            public OAuth2AccessToken convert(Response response) throws IOException {
                return getApi().getAccessTokenExtractor().extract(response);
            }
        });
    }

    public Future<OAuth2AccessToken> getAccessTokenAsync(String code) {
        return getAccessToken(code, null, null);
    }

    /**
     * @param code code
     * @param pkceCodeVerifier pkceCodeVerifier
     * @return future
     * @deprecated use {@link #getAccessTokenAsync(com.github.scribejava.core.oauth.AccessTokenRequestParams) }
     */
    @Deprecated
    public Future<OAuth2AccessToken> getAccessTokenAsync(String code, String pkceCodeVerifier) {
        return getAccessToken(code, null, pkceCodeVerifier);
    }

    public Future<OAuth2AccessToken> getAccessTokenAsync(AccessTokenRequestParams params) {
        return getAccessToken(params, null);
    }

    public OAuth2AccessToken getAccessToken(String code) throws IOException, InterruptedException, ExecutionException {
        return getAccessToken(code, (String) null);
    }

    /**
     * @param code code
     * @param pkceCodeVerifier pkceCodeVerifier
     * @return token
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     * @throws ExecutionException ExecutionException
     *
     * @deprecated use {@link #getAccessToken(com.github.scribejava.core.oauth.AccessTokenRequestParams) }
     */
    @Deprecated
    public OAuth2AccessToken getAccessToken(String code, String pkceCodeVerifier)
            throws IOException, InterruptedException, ExecutionException {
        final OAuthRequest request = createAccessTokenRequest(code, pkceCodeVerifier);

        return sendAccessTokenRequestSync(request);
    }

    public OAuth2AccessToken getAccessToken(AccessTokenRequestParams params)
            throws IOException, InterruptedException, ExecutionException {
        return sendAccessTokenRequestSync(createAccessTokenRequest(params));
    }

    /**
     * Start the request to retrieve the access token. The optionally provided callback will be called with the Token
     * when it is available.
     *
     * @param params params
     * @param callback optional callback
     * @return Future
     */
    public Future<OAuth2AccessToken> getAccessToken(AccessTokenRequestParams params,
            OAuthAsyncRequestCallback<OAuth2AccessToken> callback) {
        return sendAccessTokenRequestAsync(createAccessTokenRequest(params), callback);
    }

    /**
     * @param code code
     * @param callback callback
     * @param pkceCodeVerifier pkceCodeVerifier
     * @return future
     *
     * @deprecated use {@link #getAccessToken(com.github.scribejava.core.oauth.AccessTokenRequestParams,
     * com.github.scribejava.core.model.OAuthAsyncRequestCallback) }
     */
    @Deprecated
    public Future<OAuth2AccessToken> getAccessToken(String code, OAuthAsyncRequestCallback<OAuth2AccessToken> callback,
            String pkceCodeVerifier) {
        final OAuthRequest request = createAccessTokenRequest(code, pkceCodeVerifier);

        return sendAccessTokenRequestAsync(request, callback);
    }

    public Future<OAuth2AccessToken> getAccessToken(String code,
            OAuthAsyncRequestCallback<OAuth2AccessToken> callback) {

        return getAccessToken(code, callback, null);
    }

    /**
     * @param code code
     * @return request
     *
     * @deprecated use {@link #createAccessTokenRequest(com.github.scribejava.core.oauth.AccessTokenRequestParams)}
     */
    @Deprecated
    protected OAuthRequest createAccessTokenRequest(String code) {
        return createAccessTokenRequest(AccessTokenRequestParams.create(code));
    }

    /**
     *
     * @param code code
     * @param pkceCodeVerifier pkceCodeVerifier
     * @return request
     *
     * @deprecated use {@link #createAccessTokenRequest(com.github.scribejava.core.oauth.AccessTokenRequestParams)}
     */
    @Deprecated
    protected OAuthRequest createAccessTokenRequest(String code, String pkceCodeVerifier) {
        return createAccessTokenRequest(AccessTokenRequestParams.create(code).pkceCodeVerifier(pkceCodeVerifier));
    }

    protected OAuthRequest createAccessTokenRequest(AccessTokenRequestParams params) {
        final OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());

        api.getClientAuthentication().addClientAuthentication(request, getApiKey(), getApiSecret());

        request.addParameter(OAuthConstants.CODE, params.getCode());
        final String callback = getCallback();
        if (callback != null) {
            request.addParameter(OAuthConstants.REDIRECT_URI, callback);
        }
        final String scope = params.getScope();
        if (scope != null) {
            request.addParameter(OAuthConstants.SCOPE, scope);
        } else if (defaultScope != null) {
            request.addParameter(OAuthConstants.SCOPE, defaultScope);
        }
        request.addParameter(OAuthConstants.GRANT_TYPE, OAuthConstants.AUTHORIZATION_CODE);

        final String pkceCodeVerifier = params.getPkceCodeVerifier();
        if (pkceCodeVerifier != null) {
            request.addParameter(PKCE.PKCE_CODE_VERIFIER_PARAM, pkceCodeVerifier);
        }
        return request;
    }

    public Future<OAuth2AccessToken> refreshAccessTokenAsync(String refreshToken) {
        return refreshAccessToken(refreshToken, (OAuthAsyncRequestCallback<OAuth2AccessToken>) null);
    }

    public Future<OAuth2AccessToken> refreshAccessTokenAsync(String refreshToken, String scope) {
        return refreshAccessToken(refreshToken, scope, null);
    }

    public OAuth2AccessToken refreshAccessToken(String refreshToken)
            throws IOException, InterruptedException, ExecutionException {
        final OAuthRequest request = createRefreshTokenRequest(refreshToken);

        return sendAccessTokenRequestSync(request);
    }

    public OAuth2AccessToken refreshAccessToken(String refreshToken, String scope)
            throws IOException, InterruptedException, ExecutionException {
        final OAuthRequest request = createRefreshTokenRequest(refreshToken, scope);

        return sendAccessTokenRequestSync(request);
    }

    public Future<OAuth2AccessToken> refreshAccessToken(String refreshToken,
            OAuthAsyncRequestCallback<OAuth2AccessToken> callback) {
        final OAuthRequest request = createRefreshTokenRequest(refreshToken);

        return sendAccessTokenRequestAsync(request, callback);
    }

    public Future<OAuth2AccessToken> refreshAccessToken(String refreshToken, String scope,
            OAuthAsyncRequestCallback<OAuth2AccessToken> callback) {
        final OAuthRequest request = createRefreshTokenRequest(refreshToken, scope);

        return sendAccessTokenRequestAsync(request, callback);
    }

    /**
     * @param refreshToken refreshToken
     * @return request
     *
     * @deprecated use {@link #createRefreshTokenRequest(java.lang.String, java.lang.String) }
     */
    @Deprecated
    protected OAuthRequest createRefreshTokenRequest(String refreshToken) {
        return createRefreshTokenRequest(refreshToken, null);
    }

    protected OAuthRequest createRefreshTokenRequest(String refreshToken, String scope) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("The refreshToken cannot be null or empty");
        }
        final OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getRefreshTokenEndpoint());

        api.getClientAuthentication().addClientAuthentication(request, getApiKey(), getApiSecret());

        if (scope != null) {
            request.addParameter(OAuthConstants.SCOPE, scope);
        } else if (defaultScope != null) {
            request.addParameter(OAuthConstants.SCOPE, defaultScope);
        }

        request.addParameter(OAuthConstants.REFRESH_TOKEN, refreshToken);
        request.addParameter(OAuthConstants.GRANT_TYPE, OAuthConstants.REFRESH_TOKEN);
        return request;
    }

    public OAuth2AccessToken getAccessTokenPasswordGrant(String uname, String password)
            throws IOException, InterruptedException, ExecutionException {
        final OAuthRequest request = createAccessTokenPasswordGrantRequest(uname, password);

        return sendAccessTokenRequestSync(request);
    }

    public OAuth2AccessToken getAccessTokenPasswordGrant(String uname, String password, String scope)
            throws IOException, InterruptedException, ExecutionException {
        final OAuthRequest request = createAccessTokenPasswordGrantRequest(uname, password, scope);

        return sendAccessTokenRequestSync(request);
    }

    public Future<OAuth2AccessToken> getAccessTokenPasswordGrantAsync(String uname, String password) {
        return getAccessTokenPasswordGrantAsync(uname, password, (OAuthAsyncRequestCallback<OAuth2AccessToken>) null);
    }

    public Future<OAuth2AccessToken> getAccessTokenPasswordGrantAsync(String uname, String password, String scope) {
        return getAccessTokenPasswordGrantAsync(uname, password, scope, null);
    }

    /**
     * Request Access Token Password Grant async version
     *
     * @param uname User name
     * @param password User password
     * @param callback Optional callback
     * @return Future
     */
    public Future<OAuth2AccessToken> getAccessTokenPasswordGrantAsync(String uname, String password,
            OAuthAsyncRequestCallback<OAuth2AccessToken> callback) {
        final OAuthRequest request = createAccessTokenPasswordGrantRequest(uname, password);

        return sendAccessTokenRequestAsync(request, callback);
    }

    public Future<OAuth2AccessToken> getAccessTokenPasswordGrantAsync(String uname, String password, String scope,
            OAuthAsyncRequestCallback<OAuth2AccessToken> callback) {
        final OAuthRequest request = createAccessTokenPasswordGrantRequest(uname, password, scope);

        return sendAccessTokenRequestAsync(request, callback);
    }

    /**
     *
     * @param username username
     * @param password password
     * @return request
     *
     * @deprecated use {@link #createAccessTokenPasswordGrantRequest(java.lang.String, java.lang.String,
     * java.lang.String) }
     */
    @Deprecated
    protected OAuthRequest createAccessTokenPasswordGrantRequest(String username, String password) {
        return createAccessTokenPasswordGrantRequest(username, password, null);
    }

    protected OAuthRequest createAccessTokenPasswordGrantRequest(String username, String password, String scope) {
        final OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());
        request.addParameter(OAuthConstants.USERNAME, username);
        request.addParameter(OAuthConstants.PASSWORD, password);

        if (scope != null) {
            request.addParameter(OAuthConstants.SCOPE, scope);
        } else if (defaultScope != null) {
            request.addParameter(OAuthConstants.SCOPE, defaultScope);
        }

        request.addParameter(OAuthConstants.GRANT_TYPE, OAuthConstants.PASSWORD);

        api.getClientAuthentication().addClientAuthentication(request, getApiKey(), getApiSecret());

        return request;
    }

    public Future<OAuth2AccessToken> getAccessTokenClientCredentialsGrantAsync() {
        return getAccessTokenClientCredentialsGrant((OAuthAsyncRequestCallback<OAuth2AccessToken>) null);
    }

    public Future<OAuth2AccessToken> getAccessTokenClientCredentialsGrantAsync(String scope) {
        return getAccessTokenClientCredentialsGrant(scope, null);
    }

    public OAuth2AccessToken getAccessTokenClientCredentialsGrant()
            throws IOException, InterruptedException, ExecutionException {
        final OAuthRequest request = createAccessTokenClientCredentialsGrantRequest();

        return sendAccessTokenRequestSync(request);
    }

    public OAuth2AccessToken getAccessTokenClientCredentialsGrant(String scope)
            throws IOException, InterruptedException, ExecutionException {
        final OAuthRequest request = createAccessTokenClientCredentialsGrantRequest(scope);

        return sendAccessTokenRequestSync(request);
    }

    /**
     * Start the request to retrieve the access token using client-credentials grant. The optionally provided callback
     * will be called with the Token when it is available.
     *
     * @param callback optional callback
     * @return Future
     */
    public Future<OAuth2AccessToken> getAccessTokenClientCredentialsGrant(
            OAuthAsyncRequestCallback<OAuth2AccessToken> callback) {
        final OAuthRequest request = createAccessTokenClientCredentialsGrantRequest();

        return sendAccessTokenRequestAsync(request, callback);
    }

    public Future<OAuth2AccessToken> getAccessTokenClientCredentialsGrant(String scope,
            OAuthAsyncRequestCallback<OAuth2AccessToken> callback) {
        final OAuthRequest request = createAccessTokenClientCredentialsGrantRequest(scope);

        return sendAccessTokenRequestAsync(request, callback);
    }

    /**
     * @return request
     *
     * @deprecated use {@link #createAccessTokenClientCredentialsGrantRequest(java.lang.String) }
     */
    @Deprecated
    protected OAuthRequest createAccessTokenClientCredentialsGrantRequest() {
        return createAccessTokenClientCredentialsGrantRequest(null);
    }

    protected OAuthRequest createAccessTokenClientCredentialsGrantRequest(String scope) {
        final OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());

        api.getClientAuthentication().addClientAuthentication(request, getApiKey(), getApiSecret());

        if (scope != null) {
            request.addParameter(OAuthConstants.SCOPE, scope);
        } else if (defaultScope != null) {
            request.addParameter(OAuthConstants.SCOPE, defaultScope);
        }
        request.addParameter(OAuthConstants.GRANT_TYPE, OAuthConstants.CLIENT_CREDENTIALS);
        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return VERSION;
    }

    public void signRequest(String accessToken, OAuthRequest request) {
        api.getBearerSignature().signRequest(accessToken, request);
    }

    public void signRequest(OAuth2AccessToken accessToken, OAuthRequest request) {
        signRequest(accessToken == null ? null : accessToken.getAccessToken(), request);
    }

    /**
     * @return AuthorizationUrlWithPKCE
     *
     * @deprecated use new builder pattern {@link AuthorizationUrlBuilder}
     */
    @Deprecated
    public AuthorizationUrlWithPKCE getAuthorizationUrlWithPKCE() {
        final AuthorizationUrlBuilder authorizationUrlBuilder = createAuthorizationUrlBuilder()
                .initPKCE();

        return new AuthorizationUrlWithPKCE(authorizationUrlBuilder.getPkce(), authorizationUrlBuilder.build());
    }

    /**
     * @param state state
     * @return AuthorizationUrlWithPKCE
     *
     * @deprecated use new builder pattern {@link AuthorizationUrlBuilder}
     */
    @Deprecated
    public AuthorizationUrlWithPKCE getAuthorizationUrlWithPKCE(String state) {
        final AuthorizationUrlBuilder authorizationUrlBuilder = createAuthorizationUrlBuilder()
                .state(state)
                .initPKCE();

        return new AuthorizationUrlWithPKCE(authorizationUrlBuilder.getPkce(), authorizationUrlBuilder.build());
    }

    /**
     * @param additionalParams additionalParams
     * @return AuthorizationUrlWithPKCE
     *
     * @deprecated use new builder pattern {@link AuthorizationUrlBuilder}
     */
    @Deprecated
    public AuthorizationUrlWithPKCE getAuthorizationUrlWithPKCE(Map<String, String> additionalParams) {
        final AuthorizationUrlBuilder authorizationUrlBuilder = createAuthorizationUrlBuilder()
                .additionalParams(additionalParams)
                .initPKCE();

        return new AuthorizationUrlWithPKCE(authorizationUrlBuilder.getPkce(), authorizationUrlBuilder.build());
    }

    /**
     * @param state state
     * @param additionalParams additionalParams
     * @return AuthorizationUrlWithPKCE
     *
     * @deprecated use new builder pattern {@link AuthorizationUrlBuilder}
     */
    @Deprecated
    public AuthorizationUrlWithPKCE getAuthorizationUrlWithPKCE(String state, Map<String, String> additionalParams) {
        final AuthorizationUrlBuilder authorizationUrlBuilder = createAuthorizationUrlBuilder()
                .state(state)
                .additionalParams(additionalParams)
                .initPKCE();

        return new AuthorizationUrlWithPKCE(authorizationUrlBuilder.getPkce(), authorizationUrlBuilder.build());
    }

    /**
     * Returns the URL where you should redirect your users to authenticate your application.
     *
     * @return the URL where you should redirect your users
     */
    public String getAuthorizationUrl() {
        return createAuthorizationUrlBuilder().build();
    }

    public String getAuthorizationUrl(String state) {
        return createAuthorizationUrlBuilder()
                .state(state)
                .build();
    }

    /**
     * Returns the URL where you should redirect your users to authenticate your application.
     *
     * @param additionalParams any additional GET params to add to the URL
     * @return the URL where you should redirect your users
     */
    public String getAuthorizationUrl(Map<String, String> additionalParams) {
        return createAuthorizationUrlBuilder()
                .additionalParams(additionalParams)
                .build();
    }

    /**
     *
     * @param state state
     * @param additionalParams additionalParams
     * @return url
     *
     * @deprecated use new builder pattern {@link AuthorizationUrlBuilder}
     */
    @Deprecated
    public String getAuthorizationUrl(String state, Map<String, String> additionalParams) {
        return createAuthorizationUrlBuilder()
                .state(state)
                .additionalParams(additionalParams)
                .build();
    }

    public String getAuthorizationUrl(PKCE pkce) {
        return createAuthorizationUrlBuilder()
                .pkce(pkce)
                .build();
    }

    /**
     * @param state state
     * @param pkce pkce
     * @return url
     *
     * @deprecated use new builder pattern {@link AuthorizationUrlBuilder}
     */
    @Deprecated
    public String getAuthorizationUrl(String state, PKCE pkce) {
        return createAuthorizationUrlBuilder()
                .state(state)
                .pkce(pkce)
                .build();
    }

    /**
     * @param additionalParams additionalParams
     * @param pkce pkce
     * @return url
     *
     * @deprecated use new builder pattern {@link AuthorizationUrlBuilder}
     */
    @Deprecated
    public String getAuthorizationUrl(Map<String, String> additionalParams, PKCE pkce) {
        return createAuthorizationUrlBuilder()
                .additionalParams(additionalParams)
                .pkce(pkce)
                .build();
    }

    /**
     *
     * @param state state
     * @param additionalParams additionalParams
     * @param pkce pkce
     * @return url
     *
     * @deprecated use new builder pattern {@link AuthorizationUrlBuilder}
     */
    @Deprecated
    public String getAuthorizationUrl(String state, Map<String, String> additionalParams, PKCE pkce) {
        return createAuthorizationUrlBuilder()
                .state(state)
                .additionalParams(additionalParams)
                .pkce(pkce)
                .build();
    }

    public AuthorizationUrlBuilder createAuthorizationUrlBuilder() {
        return new AuthorizationUrlBuilder(this);
    }

    public DefaultApi20 getApi() {
        return api;
    }

    protected OAuthRequest createRevokeTokenRequest(String tokenToRevoke, TokenTypeHint tokenTypeHint) {
        final OAuthRequest request = new OAuthRequest(Verb.POST, api.getRevokeTokenEndpoint());

        api.getClientAuthentication().addClientAuthentication(request, getApiKey(), getApiSecret());

        request.addParameter("token", tokenToRevoke);
        if (tokenTypeHint != null) {
            request.addParameter("token_type_hint", tokenTypeHint.toString());
        }
        return request;
    }

    public Future<Void> revokeTokenAsync(String tokenToRevoke) {
        return revokeTokenAsync(tokenToRevoke, null);
    }

    public Future<Void> revokeTokenAsync(String tokenToRevoke, TokenTypeHint tokenTypeHint) {
        return revokeToken(tokenToRevoke, null, tokenTypeHint);
    }

    public void revokeToken(String tokenToRevoke) throws IOException, InterruptedException, ExecutionException {
        revokeToken(tokenToRevoke, (TokenTypeHint) null);
    }

    public void revokeToken(String tokenToRevoke, TokenTypeHint tokenTypeHint)
            throws IOException, InterruptedException, ExecutionException {
        final OAuthRequest request = createRevokeTokenRequest(tokenToRevoke, tokenTypeHint);

        checkForErrorRevokeToken(execute(request));
    }

    public Future<Void> revokeToken(String tokenToRevoke, OAuthAsyncRequestCallback<Void> callback) {
        return revokeToken(tokenToRevoke, callback, null);
    }

    public Future<Void> revokeToken(String tokenToRevoke, OAuthAsyncRequestCallback<Void> callback,
            TokenTypeHint tokenTypeHint) {
        final OAuthRequest request = createRevokeTokenRequest(tokenToRevoke, tokenTypeHint);

        return execute(request, callback, new OAuthRequest.ResponseConverter<Void>() {
            @Override
            public Void convert(Response response) throws IOException {
                checkForErrorRevokeToken(response);
                return null;
            }
        });
    }

    private void checkForErrorRevokeToken(Response response) throws IOException {
        if (response.getCode() != 200) {
            OAuth2AccessTokenJsonExtractor.instance().generateError(response.getBody());
        }
    }

    public OAuth2Authorization extractAuthorization(String redirectLocation) {
        final OAuth2Authorization authorization = new OAuth2Authorization();
        int end = redirectLocation.indexOf('#');
        if (end == -1) {
            end = redirectLocation.length();
        }
        for (String param : redirectLocation.substring(redirectLocation.indexOf('?') + 1, end).split("&")) {
            final String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                switch (keyValue[0]) {
                    case "code":
                        authorization.setCode(keyValue[1]);
                        break;
                    case "state":
                        authorization.setState(keyValue[1]);
                        break;
                    default: //just ignore any other param;
                }
            }
        }
        return authorization;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getDefaultScope() {
        return defaultScope;
    }
}
