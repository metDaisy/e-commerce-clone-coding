@ApplicationModule(allowedDependencies = {"common::*", "global::jwt",
        "user::user-api", "global::blacklist", "global::login-policy"})
package io.github.metdaisy.amaazon.auth;

import org.springframework.modulith.ApplicationModule;
