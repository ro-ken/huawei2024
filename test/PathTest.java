import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.PathImpl;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PathTest {

    @Test
    public void test01(){
        Path path = new PathImpl();
        ArrayList<Point> path1 = path.getPath(new Point(36, 172), new Point(9, 147));
        int fps  = path.getPathFps(new Point(36, 172), new Point(9, 147));
        ArrayList<Point> path2 = path.getPath(new Point(1, 3), new Point(1, 3));
        System.out.println(path1);
        System.out.println(fps);
    }

    @Test
    public void test_Function_GetPath() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get("test\\map1.txt"));
        char[][] inputmap = new char[lines.size()][];
        for (int i = 0; i < lines.size(); i++) {
//                System.out.println(lines.get(i).toCharArray());
            inputmap[i] = lines.get(i).toCharArray();
        }
        Mapinfo.init(inputmap);

        // 初始化路径实例
        Path path = new PathImpl();

        ArrayList<Point> path1 = path.getPath(new Point(7, 14), new Point(3, 25));
        ArrayList<Point> path2 = path.getPath(new Point(1, 3), new Point(6, 9));
        int fps  = path.getPathFps(new Point(7, 14), new Point(3, 25));
        System.out.println(path1);
        System.out.println(fps);
    }
}
