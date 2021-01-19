package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        System.exit(textui.runClasses(UnitTest.class));
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void placeholderTest() {
    }

    @Test
    public void test() {
        List<String> commits = new ArrayList<>(),
                commitstwo = new ArrayList<>();
        commits.add("first");
        commits.add("second");
        commits.add("third");
        commitstwo.add("second");
        commitstwo.add("third");
        commitstwo.add("fourth");
        commits.retainAll(commitstwo);
        System.out.println(commits);
        commitstwo.retainAll(commits);
        System.out.println(commitstwo);
    }

}


