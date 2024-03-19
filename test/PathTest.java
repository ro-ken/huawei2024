import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.PathImpl;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PathTest {
    private static final String[] FILE_NAMES = {
            "test\\map1.txt",  // map-3.6 0
            "test\\map2.txt",  // map-3.7 1
            "test\\map3.txt",  // map-3.8 2
            "test\\map4.txt",  // map-3.9 3
            "test\\map5.txt",  // map-3.10 4
            "test\\map6.txt",  // map-3.11 5
            "test\\map7.txt",  // map-3.12 6
            "test\\map8.txt"   // map-3.13 7
    };
    //private Path path;

    private void init(int map) throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get(FILE_NAMES[map]));
            char[][] inputmap = new char[lines.size()][];
            for (int i = 0; i < lines.size(); i++) {
//                System.out.println(lines.get(i).toCharArray());
                inputmap[i] = lines.get(i).toCharArray();
            }
            Mapinfo.init(inputmap);

            // 初始化路径实例
            Path path = new PathImpl();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_Function_GetPath() throws IOException {
        int map = 1;
        init(map);
        // 初始化路径实例
        Path path = new PathImpl();
        //ArrayList<Point> path1 = path.getPath(new Point(7, 14), new Point(3, 25));
       // ArrayList<Point> path2 = path.getPath(new Point(1, 3), new Point(6, 9));
        int fps  = path.getPathFps(new Point(100, 114), new Point(15, 122));
       // System.out.println(path1);
        System.out.println(fps);
    }

    @Test
    public void test_Function_GetHidePointPath() throws IOException {
        int map = 1;
        init(map);
        // 初始化路径实例
        Path path = new PathImpl();
        Point pos = new Point(35, 74);
        List<Point> leftPath = new ArrayList<>(Arrays.asList(
                new Point(35, 76), new Point(35, 75), new Point(35, 74),
                new Point(35, 73), new Point(34, 73), new Point(34, 72),
                new Point(34, 71), new Point(33, 71), new Point(33, 70),
                new Point(33, 69), new Point(32, 69), new Point(31, 69),
                new Point(30, 69), new Point(30, 68), new Point(30, 67)
        ));
        ArrayList<Point> hidePath = path.getHidePointPath(pos, leftPath);
        for (Point point : hidePath) {
            System.out.println(point);
        }
    }
}
