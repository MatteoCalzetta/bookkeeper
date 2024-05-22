import org.apache.bookkeeper.common.component.Lifecycle;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LifecycleTest {
    Lifecycle myLifecycle = mock(Lifecycle.class);

    @Before
    public void beforeTestLifecycle() {
        when(myLifecycle.state()).thenReturn(Lifecycle.State.STARTED);
    }

    @Test
    public void testState() {
        Assert.assertNotEquals(myLifecycle.state(), Lifecycle.State.CLOSED);
    }
}