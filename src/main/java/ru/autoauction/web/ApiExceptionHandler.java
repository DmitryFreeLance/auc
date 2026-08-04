package ru.autoauction.web;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Ошибка":e.getReason()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message","Проверьте заполнение полей"));}
 @ExceptionHandler(ArithmeticException.class) ResponseEntity<?> overflow(){return ResponseEntity.badRequest().body(Map.of("message","Слишком большое значение ставки"));}
}
