@ApplicationModule(allowedDependencies = {"common::*", "user::event", "global::jwt",
        "user::user-api", "global::blacklist"})
package io.github.metdaisy.amaazon.auth;

import org.springframework.modulith.ApplicationModule;
