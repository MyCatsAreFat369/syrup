package org.maplestar.syrup.utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.maplestar.syrup.Main;
import org.maplestar.syrup.data.rank.RankingData;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * Utility class for generating images via the AWT library.
 */
public class ImageUtils {
    private static final String kiwiMaruFont = "Kiwi Maru";
    private static final String notoSansFont = "Noto Sans JP";

    /**
     * Creates an image containing a user's name, rank, level, XP amount, and remaining XP until level-up.
     * Is based on a user's banner or alternatively their avatar.
     *
     * @param member the member
     * @param rankingData the member's ranking
     * @return the image in its byte representation
     * @throws IOException if there's a problem fetching the images from Discord or encoding the newly created image
     */
    public static byte[] generateRankImage(Member member, RankingData rankingData) throws IOException {
        // Load the avatar & banner from Discord
        var profile = member.getUser().retrieveProfile().complete();
        var avatarUrl = member.getEffectiveAvatarUrl();
        var bannerUrl = profile.getBannerUrl();
        var accentColor = profile.getAccentColor();
        BufferedImage avatarImage = loadMemberAvatar(member, member.getIdLong());
        BufferedImage bannerImage;

        // If there's no banner, use a cropped version of the avatar
        if (bannerUrl != null) {
            bannerImage = loadImageFromUrl(bannerUrl + "?size=1024");
        } else {
            bannerImage = new BufferedImage(1024, 360, BufferedImage.TYPE_INT_ARGB);
            var g2dBanner = bannerImage.createGraphics();
            g2dBanner.setColor(accentColor);
            g2dBanner.fillRect(0, 0, 1024, 360);
            g2dBanner.dispose();
        }

        BufferedImage preClippedImage = new BufferedImage(1600, 1300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dClip = preClippedImage.createGraphics();

        // Antialiasing & better quality images
        g2dClip.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // idk what the heck this means

        // Draw the banner
        g2dClip.setColor(new Color(15, 15, 15));
        g2dClip.fillRect(0, 0, 1600, 1300);

        g2dClip.drawImage(bannerImage, 0, 0, 1600, 562, null);

        g2dClip.dispose();

        BufferedImage image = new BufferedImage(1600, 1300, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        RoundRectangle2D.Double roundRect = new RoundRectangle2D.Double(0, 0, preClippedImage.getWidth(), preClippedImage.getHeight(), 100, 100);
        g2d.setClip(roundRect);
        g2d.drawImage(preClippedImage, 0, 0, image.getWidth(), image.getHeight(), null);

        // Draw Syrup Icon
        URL url = Main.class.getResource("/images/syrupicon.png");
        InputStream inputStream = url.openStream();
        BufferedImage imageSyrup = ImageIO.read(inputStream);
        g2d.drawImage(imageSyrup, 1375, 570, 192, 192, null);

        // Draw the avatar
        int avatarX = 52, avatarY = 200;
        int avatarWidth = 256 * 2, avatarHeight = 256 * 2;
        Ellipse2D.Double circle = new Ellipse2D.Double(avatarX, avatarY, avatarWidth, avatarHeight);
        g2d.setClip(circle); // try now
        g2d.drawImage(avatarImage, avatarX, avatarY, avatarWidth, avatarHeight, null); // AWESOME

        // Reset clip so the text isn't clipped
        g2d.setClip(null);

        // Draw the username
        int textX = 50, textY = 880;
        int nameSize = 120;
        g2d.setFont(new Font(notoSansFont, Font.BOLD, nameSize));
        g2d.setColor(Color.WHITE);
        g2d.drawString(member.getEffectiveName(), textX, textY);

        // Draw Rank + Level + XP
        int normalTextSize = 80;
        int rankingX = 70, rankingY = 1150;
        g2d.setFont(new Font(notoSansFont, Font.BOLD, normalTextSize));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Rank " + (rankingData.isInvalid() ? "Invalid" : "#" + rankingData.rank()), rankingX - 10, textY + nameSize - 25);
        g2d.drawString("Level " + rankingData.levelData().level(), rankingX, rankingY);

        BufferedImage imageXP = new BufferedImage(1500, 25, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dXP = imageXP.createGraphics();

        long remainingXP = rankingData.levelData().remainingXPForLevelup();
        long totalXP = rankingData.levelData().requiredForLevelupTotal();
        float xpRatio = ((float) (totalXP - remainingXP)) / totalXP;

        g2dXP.setColor(new Color(180, 180, 180));
        g2dXP.fillRect(0, 0, 1500, 25);

        Color xpColor;
        if (xpRatio <= 0.2) {
            xpColor = new Color(255, 0, 0);
        } else if (xpRatio <= 0.6) {
            xpColor = new Color(255, 165, 0);
        } else {
            xpColor = new Color(128, 255, 0);
        }
        g2dXP.setColor(xpColor);
        g2dXP.fillRect(0, 0, (int) (1500 * xpRatio), 25);
        g2dXP.dispose();

        int xpX = 50, xpY = 1200;
        RoundRectangle2D.Double roundRectXP = new RoundRectangle2D.Double(xpX, xpY, imageXP.getWidth(), imageXP.getHeight(), 25, 25);
        g2d.setClip(roundRectXP);
        g2d.drawImage(imageXP, xpX, xpY, imageXP.getWidth(), imageXP.getHeight(), null);
        g2d.setClip(null);

        // Draw XP remaining
        if (rankingData.levelData().level() < 420) {
            int smallerTextSize = 60;
            g2d.setFont(new Font(notoSansFont, Font.PLAIN, smallerTextSize));
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.format("%,d remaining XP", remainingXP), 1125 - ("" + remainingXP).length() * 33, xpY - 50);
        }

        // Free graphics object to save resources
        g2d.dispose();

        // Write output image
        var outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    public static byte[] generateLeaderboardImage(List<RankingData> rankedUsers, RankingData userRank,
                                                  Guild guild, int currentPage, int totalPages) throws IOException {
        BufferedImage image = new BufferedImage(2000, 1400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(new Color(43, 43, 43));
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());

        // Leaderboard Avatar and Title
        g2d.drawImage(generateAvatar(loadGuildAvatar(guild)), 60, 85, 185, 185, null);

        String titleText = "Leaderboard for " + guild.getName();
        int titleFontSize = 80;
        g2d.setFont(new Font(kiwiMaruFont, Font.BOLD, titleFontSize));
        var fontMetrics = g2d.getFontMetrics();
        int length = fontMetrics.charsWidth(titleText.toCharArray(), 0, titleText.length());
        int lengthThreshold = (int) (image.getWidth() * 0.75);
        if (length > lengthThreshold) {
            titleFontSize *= (int) (lengthThreshold / (double) length);

            g2d.setFont(new Font(kiwiMaruFont, Font.BOLD, titleFontSize));
        }

        g2d.setColor(Color.WHITE);
        g2d.drawString(titleText, 290, 200);
        // Syrup
        URL url = Main.class.getResource("/images/syrupicon.png");
        InputStream inputStream = url.openStream();
        BufferedImage imageSyrup = ImageIO.read(inputStream);
        g2d.drawImage(imageSyrup, 1710, 1110, 256, 256, null);

        // Footer (current page, total pages)
        String footerText = "Page " + currentPage + " / " + totalPages;
        g2d.setFont(new Font(notoSansFont, Font.BOLD, 30));
        fontMetrics = g2d.getFontMetrics();
        length = fontMetrics.charsWidth(footerText.toCharArray(), 0, footerText.length());
        g2d.drawString(footerText, image.getWidth() / 2 - length / 2, 1350);

        // You
        var memberYou = guild.retrieveMemberById(userRank.userID()).submit().join();
        int youX = image.getWidth() / 2 - 375;
        int youY = 1140;
        g2d.drawImage(generateLeaderboardRankImage(userRank, "You", true), youX, youY, 750, 140, null);
        g2d.drawImage(generateAvatar(loadMemberAvatar(memberYou, userRank.userID())), youX - 20, youY - 20, 160, 160, null);
        var youRankImage = generateRankNumberImage(userRank.rank());
        double scaleFactor = 0.6 - 0.3 * (("" + (userRank.rank())).length() / 5.0);
        g2d.drawImage(youRankImage,
                youX + 140 - (int) (youRankImage.getWidth() * scaleFactor / 2),
                youY + 100,
                (int) (youRankImage.getWidth() * 0.5),
                (int) (youRankImage.getHeight() * 0.5),
                null
        );

        // All ranking people
        var ids = rankedUsers.stream()
                .map(RankingData::userID)
                .toList();

        var members = guild.retrieveMembersByIds(ids).get();
        for (int i = 0; i < rankedUsers.size(); i++) {
            long userID = rankedUsers.get(i).userID();
            double setX = 220 + 784 * Math.floor((i + 0.001) / 5.0);
            double setY = 290 + 160 * (i % 5);

            Member member = null;
            for (var m : members) {
                if (m.getIdLong() == userID) {
                    member = m;
                    break;
                }
            }

            String memberName = "Unknown User (" + rankedUsers.get(i).userID() + ")";
            if (member != null) {
                memberName = member.getEffectiveName();
            }

            g2d.drawImage(generateLeaderboardRankImage(rankedUsers.get(i), memberName, rankedUsers.get(i).userID() == userRank.userID()), (int) setX, (int) setY, 750, 140, null);
            g2d.drawImage(generateAvatar(loadMemberAvatar(member, userID)), (int) setX - 10, (int) setY - 10, 160, 160, null);
        }

        for (int i = 0; i < rankedUsers.size(); i++) {
            double setX = 220 + 784 * Math.floor((i + 0.001) / 5.0);
            double setY = 290 + 160 * (i % 5);
            var rankImage = generateRankNumberImage(rankedUsers.get(i).rank());
            double scaleFactor2 = 0.6 - 0.25 * (("" + (i + 1)).length() / 5.0);
            g2d.drawImage(rankImage,
                    (int)setX + 140 - (int) (rankImage.getWidth() * scaleFactor2 / 2),
                    (int)setY + 100,
                    (int) (rankImage.getWidth() * scaleFactor2),
                    (int) (rankImage.getHeight() * scaleFactor2),
                    null
            );
        }

        BufferedImage imageClipped = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dClip = imageClipped.createGraphics();

        RoundRectangle2D.Double roundRect = new RoundRectangle2D.Double(0, 0, image.getWidth(), image.getHeight(), 300, 300);
        g2dClip.setClip(roundRect);
        g2dClip.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

        g2d.dispose();
        g2dClip.dispose();

        var outputStream = new ByteArrayOutputStream();
        ImageIO.write(imageClipped, "png", outputStream);
        return outputStream.toByteArray();
    }

    public static BufferedImage generateLeaderboardRankImage(RankingData rankingData, String name, boolean isYou) {
        BufferedImage image = new BufferedImage(800, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(new Color(126, 126, 126));
        if (isYou) g2d.setColor(new Color(102, 121, 189));
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());

        int textX = 220, textY = 65;
        double fontSize = 50;
        g2d.setFont(new Font(notoSansFont, Font.PLAIN, 50));
        var fontMetrics = g2d.getFontMetrics();

        int length = fontMetrics.charsWidth(name.toCharArray(), 0, name.length());
        int maxLength = 550;
        if (length > maxLength) {
            if (length > maxLength * 2) length = maxLength * 2;
            fontSize *= maxLength / (double) length;
        }

        g2d.setFont(new Font(notoSansFont, Font.PLAIN, (int) fontSize));
        g2d.setColor(Color.WHITE);
        g2d.drawString(name, textX, textY);

        g2d.setFont(new Font(notoSansFont, Font.PLAIN, 35));
        g2d.drawString("Level " + rankingData.levelData().level(), textX, textY + 55);
        g2d.drawString("|", textX + 200, textY + 55);
        g2d.drawString(String.format("%,d XP", rankingData.levelData().xp()), textX  + 265, textY + 55);

        // The finishing touches
        RoundRectangle2D.Double roundRect = new RoundRectangle2D.Double(0, 0, image.getWidth(), image.getHeight(), image.getHeight(), image.getHeight());
        BufferedImage imageClipped = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dClip = imageClipped.createGraphics();

        g2dClip.setClip(roundRect);
        g2dClip.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

        g2d.dispose();
        g2dClip.dispose();

        return imageClipped;
    }

    private static BufferedImage generateAvatar(BufferedImage avatarImage) {
        BufferedImage image = new BufferedImage(avatarImage.getWidth(), avatarImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, avatarImage.getWidth(), avatarImage.getHeight());
        g2d.setClip(circle);
        g2d.drawImage(avatarImage, 0, 0, avatarImage.getWidth(), avatarImage.getHeight(), null);

        g2d.dispose();

        return image;
    }

    private static BufferedImage generateRankNumberImage(long rank) {
        Font font = new Font(notoSansFont, Font.PLAIN, 100);

        BufferedImage bufferedImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dFont = bufferedImage.createGraphics();

        String rankStr = String.valueOf(rank);
        g2dFont.setFont(font);
        FontMetrics fontMetrics = g2dFont.getFontMetrics();
        int width = fontMetrics.charsWidth(rankStr.toCharArray(), 0, rankStr.length());
        int height = fontMetrics.getHeight() - 60;

        int paddingX = 40, paddingY = 20;
        BufferedImage image = new BufferedImage(width + paddingX * 2, height + paddingY * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());

        g2d.setColor(Color.BLACK);
        g2d.setFont(font);
        g2d.drawString(rankStr, paddingX, paddingY + height);

        int minLength = Math.min(image.getWidth(), image.getHeight());
        RoundRectangle2D.Double roundRect = new RoundRectangle2D.Double(0, 0, image.getWidth(), image.getHeight(), minLength, minLength);
        BufferedImage imageClipped = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dClip = imageClipped.createGraphics();

        g2dClip.setClip(roundRect);
        g2dClip.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);

        g2dFont.dispose();
        g2d.dispose();
        g2dClip.dispose();
        return imageClipped;
    }

    private static BufferedImage loadImageFromUrl(String url) throws IOException {
        var imageURL = URI.create(url).toURL();
        return ImageIO.read(imageURL);
    }

    private static BufferedImage loadMemberAvatar(Member member, long userID) throws IOException {
        if (member == null) return loadImageFromUrl("https://cdn.discordapp.com/embed/avatars/" + (userID % 5) + ".png?size=256");
        var avatarUrl = member.getEffectiveAvatarUrl();
        return loadImageFromUrl(avatarUrl + "?size=256");
    }

    private static BufferedImage loadGuildAvatar(Guild guild) throws IOException {
        var avatarUrl = guild.getIconUrl();
        return loadImageFromUrl(avatarUrl + "?size=256");
    }
}
