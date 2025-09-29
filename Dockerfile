FROM gradle:jdk21-alpine AS build

COPY . .
RUN gradle clean installDist --no-daemon

FROM eclipse-temurin:21-alpine AS runtime

WORKDIR /app

COPY --from=build /home/gradle/build/install/Syrup/bin/Syrup bin/Syrup
COPY --from=build /home/gradle/build/install/Syrup/lib/ lib/
RUN apk add font-misc-misc
RUN apk add font-terminus font-inconsolata font-dejavu font-noto font-noto-cjk font-awesome font-noto-extra
RUN apk add font-vollkorn font-misc-cyrillic font-mutt-misc font-screen-cyrillic font-winitzki-cyrillic font-cronyx-cyrillic
RUN apk add font-noto-thai font-noto-tibetan font-ipa font-sony-misc font-jis-misc
RUN apk add font-isas-misc

ENTRYPOINT ["bin/Syrup"]
