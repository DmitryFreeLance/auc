package ru.autoauction.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImageStorageServiceTest {
  @TempDir Path directory;

  @Test void resizesAndConvertsUploadedPhoto() throws Exception {
    BufferedImage source=new BufferedImage(2400,1800,BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(source,"png",bytes);
    MockMultipartFile upload=new MockMultipartFile("media","car.png","image/png",bytes.toByteArray());
    String name=new ImageStorageService().store(upload,directory,"image/png");
    BufferedImage result=ImageIO.read(directory.resolve(name).toFile());
    assertThat(name).endsWith(".jpg");assertThat(result.getWidth()).isLessThanOrEqualTo(1920);assertThat(result.getHeight()).isLessThanOrEqualTo(1440);
  }
}
