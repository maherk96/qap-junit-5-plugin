import com.mk.fx.qa.qap.junit.extension.QAPJunitExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(QAPJunitExtension.class)
public class OrderStates {

    @ParameterizedTest
    @CsvSource({
            "1, New Order",
            "2, Partially Filled",
            "3, Filled",
            "4, Canceled",
            "5, Rejected"
    })
    @DisplayName("Test Order States")
    void testOrderStates() {
        // Test implementation goes here
        int orderState = 1; // Example order state
    }

}
