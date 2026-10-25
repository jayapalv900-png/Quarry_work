package college.quarry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.*;
@SpringBootTest(properties="quarry.demo=false")
class ApplicationContextTest {
 private static final Path HOME=create();
 private static Path create(){try{return Files.createTempDirectory("quarry-context-test-");}catch(Exception e){throw new RuntimeException(e);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("quarry.home",HOME::toString);}
 @Test void applicationStarts(){}
}
