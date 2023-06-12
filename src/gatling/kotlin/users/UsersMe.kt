package users

import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.feed
import io.gatling.javaapi.core.CoreDsl.jmesPath
import io.gatling.javaapi.core.CoreDsl.rampUsers
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

class UsersMe : Simulation() {
    private val users =
        csv("users.csv")
        .random()

    private val httpProtocol = http
        .baseUrl("http://localhost:8080/api/users")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling")

    private val getUsersMe =
        feed(users).exec(
            http("Login")
                .post("/login")
                .body(
                    StringBody(
                        """
                        {
                            "email": "#{email}",
                            "password": "#{password}"
                        }"""
                    )
                )
                .check(status().`is`(200))
                .check(jmesPath("token").ofString().saveAs("token"))
        ).pause(1)
            .exitHereIfFailed()
            .exec(
                http("Check")
                    .get("/me")
                    .header("Authorization", "Bearer #{token}")
                    .check(status().`is`(200))
            )
            .exitHere()

    private val userCheckData =
        scenario("Users checks data")
            .exec(getUsersMe)

    init {
        setUp(
            userCheckData
                .injectOpen(rampUsers(20).during(30))
        ).protocols(httpProtocol)
    }
}