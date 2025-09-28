FROM gradle:jdk21-alpine AS build

COPY . .
RUN gradle clean installDist --no-daemon

FROM eclipse-temurin:21-alpine AS runtime

WORKDIR /app

COPY --from=build /home/gradle/build/install/Syrup/bin/Syrup bin/Syrup
COPY --from=build /home/gradle/build/install/Syrup/lib/ lib/
RUN apk add font-noto-all

ENTRYPOINT ["bin/Syrup"]
