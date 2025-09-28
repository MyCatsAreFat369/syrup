FROM gradle:jdk21-alpine AS build

COPY . .
RUN gradle clean installDist --no-daemon

FROM eclipse-temurin:21-alpine AS runtime

WORKDIR /app

COPY --from=build /build/install/Syrup/bin/Syrup bin/Syrup
COPY --from=build /build/install/Syrup/lib/ lib/

ENTRYPOINT ["bin/Syrup"]
