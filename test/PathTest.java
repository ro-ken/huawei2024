import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.SimplePathImpl;
import org.junit.Test;

import java.util.ArrayList;

public class PathTest {

    @Test
    public void test01(){
        Path path = new SimplePathImpl();
        ArrayList<Point> path1 = path.getPath(new Point(36, 172), new Point(9, 147));
        ArrayList<Point> path2 = path.getPath(new Point(1, 3), new Point(1, 3));
        System.out.println(path1);
        System.out.println(path2);
    }

    @Test
    public void test02(){
        Path path = new SimplePathImpl();
        int fps1 = path.getPathFps(new Point(1, 3), new Point(5, 9));
        int fps2 = path.getPathFps(new Point(1, 3), new Point(1, 3));
        System.out.println(fps1);
        System.out.println(fps2);
    }
}