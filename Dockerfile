FROM eclipse-temurin:latest AS build

COPY . .
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:latest AS runtime

WORKDIR /app

COPY --from=build /build/install/Syrup/bin/Syrup bin/Syrup
COPY --from=build /build/install/Syrup/lib/ lib/

ENTRYPOINT ["bin/Syrup"]
