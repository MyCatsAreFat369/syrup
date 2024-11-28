package org.maplestar.syrup.utils;

import net.dv8tion.jda.api.entities.Member;
import org.maplestar.syrup.data.rank.RankingData;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

public class ImageUtils {
    public static byte[] generateImage(Member member, RankingData rankingData) throws IOException {
        // Load the avatar & banner from Discord
        var profile = member.getUser().retrieveProfile().complete();
        var avatarUrl = member.getEffectiveAvatarUrl();
        var bannerUrl = profile.getBannerUrl();
        BufferedImage avatarImage = loadImageFromUrl(avatarUrl + "?size=256");
        BufferedImage bannerImage;
        if (bannerUrl != null) {
            bannerImage = loadImageFromUrl(bannerUrl + "?size=1024");
        } else {
            bannerImage = new BufferedImage(1024, 360, BufferedImage.TYPE_INT_ARGB);
            BufferedImage bannerImageTemp = loadImageFromUrl(avatarUrl + "?size=256");
            var g2dBanner = bannerImage.createGraphics();
            g2dBanner.drawImage(bannerImageTemp, 0, -332, 1024, 1024, null);
            g2dBanner.dispose();
        }

        // LOGIC:
        // if Server Banner: loadServerBanner
        // else if Global Banner: loadGlobalBanner
        // else: loadAvatar, resize: 1024, position to show the middle of the avatar, so probably do pos = (0, -512) note the negative sign

        // Darken
        float scaleFactor = 0.5f;
        float[] scales = { scaleFactor, scaleFactor, scaleFactor, 1.0f };
        float[] offsets = new float[4];

        RescaleOp rescaleOp = new RescaleOp(scales, offsets, null);
        BufferedImage bannerDarkened = rescaleOp.filter(bannerImage, null);

        // Blur
        int blurSize = 16;
        float blurScale = 1f / (blurSize * blurSize);
        float[] blurKernel = new float[blurSize * blurSize];
        Arrays.fill(blurKernel, blurScale);
        Kernel kernel = new Kernel(blurSize, blurSize, blurKernel);
        ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        int pad = blurSize / 2;
        BufferedImage bannerPadded = new BufferedImage(bannerImage.getWidth() + pad * 2, bannerImage.getHeight() + pad * 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2dPadding = bannerPadded.createGraphics();

        g2dPadding.setComposite(AlphaComposite.Clear);
        g2dPadding.fillRect(0, 0, bannerImage.getWidth(), bannerImage.getHeight());
        g2dPadding.setComposite(AlphaComposite.SrcOver);

        g2dPadding.drawImage(bannerDarkened, pad, pad, null);
        g2dPadding.dispose();

        BufferedImage bannerBlurredPadded = convolveOp.filter(bannerPadded, null);

        BufferedImage bannerBlurred = bannerBlurredPadded.getSubimage(pad, pad, bannerImage.getWidth(), bannerImage.getHeight());

        BufferedImage image = new BufferedImage(bannerImage.getWidth(), bannerImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Antialiasing & better quality images
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // idk what the heck this means

        // Draw the banner
        RoundRectangle2D.Double roundRect = new RoundRectangle2D.Double(0, 0, bannerImage.getWidth(), bannerImage.getHeight(), 100, 100);
        g2d.setClip(roundRect);
        g2d.drawImage(bannerBlurred, 0, 0, null);

        // Draw the avatar
        int avatarX = 52, avatarY = 52;
        Ellipse2D.Double circle = new Ellipse2D.Double(avatarX, avatarY, avatarImage.getWidth(), avatarImage.getHeight());
        g2d.setClip(circle); // try now
        g2d.drawImage(avatarImage, avatarX, avatarY, null); // AWESOME

        // Reset clip so the text isn't clipped
        g2d.setClip(null);

        // Draw the username
        int textX = 2 * avatarX + 256;
        int textY = 100;
        int nameSize = 60;
        g2d.setFont(new Font("NotoSans", Font.BOLD, nameSize));
        g2d.setColor(member.getUser().retrieveProfile().complete().getAccentColor());
        g2d.drawString(member.getEffectiveName(), textX, textY);

        // Draw Rank + Level + XP
        int normalTextSize = 50;
        g2d.setFont(new Font("NotoSans", Font.PLAIN, normalTextSize));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Rank " + (rankingData.rank() == -1 ? "Invalid" : "#" + rankingData.rank()), textX, textY + nameSize);
        g2d.drawString("Level " + rankingData.levelData().level(), textX, textY + nameSize + normalTextSize);
        g2d.drawString(String.format("%,d XP", rankingData.levelData().xp()), textX, textY + nameSize + normalTextSize * 2);

        // Draw XP remaining
        int smallerTextSize = 30;
        long requiredXP = rankingData.levelData().requiredForLevelup(rankingData.levelData().level()) - rankingData.levelData().xp();
        g2d.setFont(new Font("NotoSans", Font.ITALIC, smallerTextSize));
        g2d.drawString(String.format("%,d XP until next level", requiredXP), textX, textY + nameSize + normalTextSize * 3);

        // Free graphics object to save resources
        g2d.dispose();

        // Write output image
        var outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private static BufferedImage loadImageFromUrl(String url) throws IOException {
        var imageURL = URI.create(url).toURL();
        return ImageIO.read(imageURL);
    }
}
