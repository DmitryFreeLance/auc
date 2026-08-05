package ru.autoauction.web;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
@RestControllerAdvice public class ApiExceptionHandler {
 private static final Logger log=LoggerFactory.getLogger(ApiExceptionHandler.class);
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e,HttpServletRequest request){if(request.getRequestURI().endsWith("/events"))return ResponseEntity.status(e.getStatusCode()).build();return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Ошибка":e.getReason()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message","Проверьте заполнение полей"));}
 @ExceptionHandler(ArithmeticException.class) ResponseEntity<?> overflow(){return ResponseEntity.badRequest().body(Map.of("message","Слишком большое значение ставки"));}
 @ExceptionHandler(MissingServletRequestParameterException.class) ResponseEntity<?> missingParameter(MissingServletRequestParameterException e){return ResponseEntity.badRequest().body(Map.of("message","Не заполнено обязательное поле: "+e.getParameterName()));}
 @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> uploadTooLarge(){return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("message","Файл слишком большой. Максимум 250 МБ на файл"));}
 @ExceptionHandler(MultipartException.class) ResponseEntity<?> multipart(){return ResponseEntity.badRequest().body(Map.of("message","Не удалось загрузить файл. Выберите его ещё раз"));}
 @ExceptionHandler(MissingServletRequestPartException.class) ResponseEntity<?> missingPart(MissingServletRequestPartException e){return ResponseEntity.badRequest().body(Map.of("message","Не выбран файл для загрузки"));}
 @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<?> unreadable(){return ResponseEntity.badRequest().body(Map.of("message","Не удалось прочитать данные формы. Обновите страницу и попробуйте ещё раз"));}
 @ExceptionHandler(MethodArgumentTypeMismatchException.class) ResponseEntity<?> wrongType(){return ResponseEntity.badRequest().body(Map.of("message","Одно из полей заполнено неверно"));}
 @ExceptionHandler(AsyncRequestNotUsableException.class) void disconnectedClient(){}
 @ExceptionHandler(ClientAbortException.class) void clientClosedMedia(){}
 @ExceptionHandler(NoResourceFoundException.class) ResponseEntity<Void> missingResource(){return ResponseEntity.notFound().build();}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e){log.error("Необработанная ошибка API",e);return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message","Не удалось выполнить действие. Попробуйте ещё раз"));}
}
