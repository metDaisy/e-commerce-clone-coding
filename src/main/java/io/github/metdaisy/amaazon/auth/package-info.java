@ApplicationModule(allowedDependencies = {"common::*", "global::jwt",
        "user::user-api", "user::signup", "global::blacklist", "global::login-policy"})
package io.github.metdaisy.amaazon.auth;

import org.springframework.modulith.ApplicationModule;
