FROM gradle:jdk21-alpine AS build

COPY . .
RUN gradle clean installDist --no-daemon

FROM eclipse-temurin:21-alpine AS runtime

WORKDIR /app

COPY --from=build /home/gradle/build/install/Syrup/bin/Syrup bin/Syrup
COPY --from=build /home/gradle/build/install/Syrup/lib/ lib/
RUN apk add font-arabic-misc font-awesome font-cronyx-cyrillic font-dejavu font-inconsolata font-ipa font-isas-misc font-jis-misc \
    font-misc-cyrillic font-misc-misc font-mutt-misc font-noto font-noto-arabic font-noto-armenian font-noto-cherokee font-noto-cjk \
    font-noto-devanagari font-noto-emoji font-noto-ethiopic font-noto-extra font-noto-georgian font-noto-hebrew font-noto-lao  \
    font-noto-malayalam font-noto-tamil font-noto-thaana font-noto-thai font-noto-tibetan font-screen-cyrillic font-sony-misc  \
    font-terminus font-vollkorn font-winitzki-cyrillic

ENTRYPOINT ["bin/Syrup"]
