package ru.autoauction.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class ImageStorageService {
  private static final int MAX_WIDTH=1920;
  private static final int MAX_HEIGHT=1440;
  private static final float JPEG_QUALITY=.84f;

  public String store(MultipartFile file,Path directory,String contentType) throws IOException {
    if("image/gif".equals(contentType)||"image/webp".equals(contentType))return copy(file,directory,"image/gif".equals(contentType)?".gif":".webp");
    BufferedImage source=ImageIO.read(file.getInputStream());
    if(source==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Не удалось обработать изображение");
    int width=source.getWidth(),height=source.getHeight();
    if(width<=0||height<=0||(long)width*height>60_000_000L)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Изображение слишком большое");
    double scale=Math.min(1d,Math.min((double)MAX_WIDTH/width,(double)MAX_HEIGHT/height));
    int targetWidth=Math.max(1,(int)Math.round(width*scale)),targetHeight=Math.max(1,(int)Math.round(height*scale));
    BufferedImage optimized=new BufferedImage(targetWidth,targetHeight,BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics=optimized.createGraphics();
    graphics.setColor(Color.WHITE);graphics.fillRect(0,0,targetWidth,targetHeight);
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
    graphics.drawImage(source,0,0,targetWidth,targetHeight,null);graphics.dispose();
    String name=UUID.randomUUID()+".jpg";Path target=directory.resolve(name);
    ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
    try(ImageOutputStream output=ImageIO.createImageOutputStream(Files.newOutputStream(target))){ImageWriteParam params=writer.getDefaultWriteParam();params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);params.setCompressionQuality(JPEG_QUALITY);writer.setOutput(output);writer.write(null,new IIOImage(optimized,null,null),params);}finally{writer.dispose();}
    return name;
  }

  private String copy(MultipartFile file,Path directory,String extension) throws IOException {String name=UUID.randomUUID()+extension;Files.copy(file.getInputStream(),directory.resolve(name),StandardCopyOption.REPLACE_EXISTING);return name;}
}
