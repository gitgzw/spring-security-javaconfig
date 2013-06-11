

/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.security.config.annotation.web

import javax.servlet.http.HttpSession

import org.springframework.context.annotation.Configuration
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.config.annotation.BaseSpringSpec
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint

/**
 * Tests to verify that {@link DefaultLoginPageConfigurator} works
 *
 * @author Rob Winch
 *
 */
public class DefaultLoginPageConfiguratorTests extends BaseSpringSpec {
    FilterChainProxy springSecurityFilterChain
    MockHttpServletRequest request
    MockHttpServletResponse response
    MockFilterChain chain

    def setup() {
        request = new MockHttpServletRequest(method:"GET")
        response = new MockHttpServletResponse()
        chain = new MockFilterChain()
    }

    def "http/form-login default login generating page"() {
        setup:
            loadConfig(DefaultLoginPageConfig)
            springSecurityFilterChain = context.getBean(FilterChainProxy)
        when:
            springSecurityFilterChain.doFilter(request,response,chain)
        then:
            findFilter(DefaultLoginPageGeneratingFilter)
            response.getRedirectedUrl() == "http://localhost/login"
        when: "request the login page"
            setup()
            request.requestURI = "/login"
            springSecurityFilterChain.doFilter(request,response,chain)
        then:
            response.getContentAsString() == """<html><head><title>Login Page</title></head><body onload='document.f.username.focus();'>
<h3>Login with Username and Password</h3><form name='f' action='/login' method='POST'>
 <table>
    <tr><td>User:</td><td><input type='text' name='username' value=''></td></tr>
    <tr><td>Password:</td><td><input type='password' name='password'/></td></tr>
    <tr><td colspan='2'><input name="submit" type="submit" value="Login"/></td></tr>
  </table>
</form></body></html>"""
        when: "fail to log in"
            setup()
            request.requestURI = "/login"
            request.method = "POST"
            springSecurityFilterChain.doFilter(request,response,chain)
        then: "sent to login error page"
            response.getRedirectedUrl() == "/login?error"
        when: "request the error page"
            HttpSession session = request.session
            setup()
            request.session = session
            request.requestURI = "/login"
            request.queryString = "error"
            springSecurityFilterChain.doFilter(request,response,chain)
        then:
            response.getContentAsString() == """<html><head><title>Login Page</title></head><body onload='document.f.username.focus();'>
<p><font color='red'>Your login attempt was not successful, try again.<br/><br/>Reason: Bad credentials</font></p><h3>Login with Username and Password</h3><form name='f' action='/login' method='POST'>
 <table>
    <tr><td>User:</td><td><input type='text' name='username' value=''></td></tr>
    <tr><td>Password:</td><td><input type='password' name='password'/></td></tr>
    <tr><td colspan='2'><input name="submit" type="submit" value="Login"/></td></tr>
  </table>
</form></body></html>"""
        when: "login success"
            setup()
            request.requestURI = "/login"
            request.method = "POST"
            request.parameters.username = ["user"] as String[]
            request.parameters.password = ["password"] as String[]
            springSecurityFilterChain.doFilter(request,response,chain)
        then: "sent to default succes page"
            response.getRedirectedUrl() == "/"
    }

    @Configuration
    static class DefaultLoginPageConfig extends BaseWebConfig {
        @Override
        protected void configure(HttpConfiguration http) {
            http
                .authorizeUrls()
                    .anyRequest().hasRole("USER")
                    .and()
                .formLogin()
        }
    }

    def "http/form-login default login with remember me"() {
        setup:
            loadConfig(DefaultLoginPageWithRememberMeConfig)
            springSecurityFilterChain = context.getBean(FilterChainProxy)
        when: "request the login page"
            setup()
            request.requestURI = "/login"
            springSecurityFilterChain.doFilter(request,response,chain)
        then:
            response.getContentAsString() == """<html><head><title>Login Page</title></head><body onload='document.f.username.focus();'>
<h3>Login with Username and Password</h3><form name='f' action='/login' method='POST'>
 <table>
    <tr><td>User:</td><td><input type='text' name='username' value=''></td></tr>
    <tr><td>Password:</td><td><input type='password' name='password'/></td></tr>
    <tr><td><input type='checkbox' name='remember-me'/></td><td>Remember me on this computer.</td></tr>
    <tr><td colspan='2'><input name="submit" type="submit" value="Login"/></td></tr>
  </table>
</form></body></html>"""
    }

    @Configuration
    static class DefaultLoginPageWithRememberMeConfig extends BaseWebConfig {
        @Override
        protected void configure(HttpConfiguration http) {
            http
                .authorizeUrls()
                    .anyRequest().hasRole("USER")
                    .and()
                .formLogin()
                    .and()
                .rememberMe()
        }
    }

    def "http/form-login default login with openid"() {
        setup:
            loadConfig(DefaultLoginPageWithOpenIDConfig)
            springSecurityFilterChain = context.getBean(FilterChainProxy)
        when: "request the login page"
            request.requestURI = "/login"
            springSecurityFilterChain.doFilter(request,response,chain)
        then:
            response.getContentAsString() == """<html><head><title>Login Page</title></head><h3>Login with OpenID Identity</h3><form name='oidf' action='/login/openid' method='POST'>
 <table>
    <tr><td>Identity:</td><td><input type='text' size='30' name='openid_identifier'/></td></tr>
    <tr><td colspan='2'><input name="submit" type="submit" value="Login"/></td></tr>
  </table>
</form></body></html>"""
    }

    @Configuration
    static class DefaultLoginPageWithOpenIDConfig extends BaseWebConfig {
        @Override
        protected void configure(HttpConfiguration http) {
            http
                .authorizeUrls()
                    .anyRequest().hasRole("USER")
                    .and()
                .openidLogin()
        }
    }

    def "http/form-login default login with openid, form login, and rememberme"() {
        setup:
            loadConfig(DefaultLoginPageWithFormLoginOpenIDRememberMeConfig)
            springSecurityFilterChain = context.getBean(FilterChainProxy)
        when: "request the login page"
            request.requestURI = "/login"
            springSecurityFilterChain.doFilter(request,response,chain)
        then:
            response.getContentAsString() == """<html><head><title>Login Page</title></head><body onload='document.f.username.focus();'>
<h3>Login with Username and Password</h3><form name='f' action='/login' method='POST'>
 <table>
    <tr><td>User:</td><td><input type='text' name='username' value=''></td></tr>
    <tr><td>Password:</td><td><input type='password' name='password'/></td></tr>
    <tr><td><input type='checkbox' name='remember-me'/></td><td>Remember me on this computer.</td></tr>
    <tr><td colspan='2'><input name="submit" type="submit" value="Login"/></td></tr>
  </table>
</form><h3>Login with OpenID Identity</h3><form name='oidf' action='/login/openid' method='POST'>
 <table>
    <tr><td>Identity:</td><td><input type='text' size='30' name='openid_identifier'/></td></tr>
    <tr><td><input type='checkbox' name='remember-me'></td><td>Remember me on this computer.</td></tr>
    <tr><td colspan='2'><input name="submit" type="submit" value="Login"/></td></tr>
  </table>
</form></body></html>"""
    }

    @Configuration
    static class DefaultLoginPageWithFormLoginOpenIDRememberMeConfig extends BaseWebConfig {
        @Override
        protected void configure(HttpConfiguration http) {
            http
                .authorizeUrls()
                    .anyRequest().hasRole("USER")
                    .and()
                .rememberMe()
                    .and()
                .formLogin()
                    .and()
                .openidLogin()
        }
    }

    def "default login with custom AuthenticationEntryPoint"() {
        when:
            loadConfig(DefaultLoginWithCustomAuthenticationEntryPointConfig)
        then:
            !findFilter(DefaultLoginPageGeneratingFilter)
    }

    @Configuration
    static class DefaultLoginWithCustomAuthenticationEntryPointConfig extends BaseWebConfig {
        @Override
        protected void configure(HttpConfiguration http) {
            http
                .exceptionHandling()
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                    .and()
                .authorizeUrls()
                    .anyRequest().hasRole("USER")
                    .and()
                .formLogin()
        }
    }

    def "DefaultLoginPage LifecycleManager"() {
        setup:
            LifecycleManager lifecycleManager = Mock()
        when:
            HttpConfiguration http = new HttpConfiguration(lifecycleManager, authenticationBldr)
            http
                // must set builder manually due to groovy not selecting correct method
                .apply(new DefaultLoginPageConfigurator([builder:http])).and()
                .formLogin()
                    .and()
                .build()

        then: "DefaultLoginPageGeneratingFilter is registered with LifecycleManager"
            1 * lifecycleManager.registerLifecycle(_ as DefaultLoginPageGeneratingFilter) >> {DefaultLoginPageGeneratingFilter o -> o}
    }
}
