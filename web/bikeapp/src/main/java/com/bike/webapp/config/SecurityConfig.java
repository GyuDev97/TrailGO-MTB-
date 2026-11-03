package com.bike.webapp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SuppressWarnings("deprecation")
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/", "/index", "/privacy", "/terms", "/support", "/css/**", "/js/**", "/images/**").permitAll()
                .antMatchers("/api/public/**").permitAll()
                // The app is not a browser session: it signs in through /app/auth/** and proves who it is
                // with a bearer token that AppAuthFilter checks. Calls about a rider's own data check it
                // (AppIdentity); the rest of /app/** is public as it was.
                .antMatchers("/app/**").permitAll()
                .antMatchers("/admin/login", "/admin/css/**", "/admin/js/**", "/admin/images/**").permitAll()
                .antMatchers("/admin/api/**", "/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/admin/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/admin/login?error")
                .permitAll()
            .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
            .and()
            .exceptionHandling()
                .accessDeniedPage("/403")
            .and()
            .csrf()
                .ignoringAntMatchers("/api/public/**", "/app/**");
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(
                "/css/**", "/js/**", "/imgs/**", "/images/**",
                "/admin/css/**", "/admin/js/**", "/admin/images/**");
    }
}
