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
import java.util.HashSet;
import java.util.List;

public class PathTest {
    private static final String[] FILE_NAMES = {
            "test\\map1_t.txt",     // 0
            "test\\map1.txt",       // 1
            "test\\map2.txt",       // 2
            "test\\map3.txt",       // 3
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
            Mapinfo.initSeaMap();
            // 初始化路径实例
            Path path = new PathImpl();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_Function_GetPath() throws IOException {
        int map = 3;
        init(map);
        // 初始化路径实例
        Path path = new PathImpl();
        ArrayList<Point> path1 = path.getPath(new Point(2, 195), new Point(93, 71));
        ArrayList<Point> path2 = path.getPath(new Point(93, 71), new Point(2, 195));
       // ArrayList<Point> path2 = path.getPath(new Point(1, 3), new Point(6, 9));
        int fps  = path.getPathFps(new Point(6, 5), new Point(55, 80));
        path1.forEach(System.out::print);
        System.out.println(fps);
    }

    @Test
    public void test_Function_getBoatPath() throws IOException {
        int map = 1;
        init(map);
        // 初始化路径实例
        Path path = new PathImpl();
//        ArrayList<Point> path3 = path.getBoatPath(new Point(104, 26), 0, new Point(26, 105));
//        ArrayList<Point> path2 = path.getBoatPath(new Point(100, 101), 2, new Point(105, 173));
        ArrayList<Point> path1 = path.getBoatPath(new Point(36, 98), 0, new Point(2, 196));

        path1.forEach(System.out::print);
        System.out.println();
//        path2.forEach(System.out::print);
    }

    @Test
    public void test_Function_GetHidePointPath() throws IOException {
        int map = 1;
        init(map);
        // 初始化路径实例
        Path path = new PathImpl();
        Point pos = new Point(142, 191);
        List<Point> leftPath = new ArrayList<>(Arrays.asList(
                new Point(141,190), new Point(142,190), new Point(142,191),
                new Point(143,191), new Point(143,192), new Point(144,192),
                new Point(144,193), new Point(145,193), new Point(145,194),
                new Point(146,194), new Point(146,195), new Point(147,195),
                new Point(147,196), new Point(148,196), new Point(149,196),
                new Point(150,196), new Point(151,196), new Point(151,195)
        ));
        ArrayList<Point> hidePath = path.getHidePointPath(pos, leftPath);
        for (Point point : hidePath) {
            System.out.println(point);
        }
    }

    @Test
    public void test_Function_getPathWithBarrierWithLimit() throws IOException {
        int map = 0;
        init(map);
        // 初始化路径实例
        Path path = new PathImpl();
        Point pos = new Point(142, 191);
        Point target = new Point(160, 190);
        HashSet<Point> leftPath = new HashSet<>(Arrays.asList(
                new Point(141,190), new Point(142,190), new Point(142,191),
                new Point(143,191), new Point(143,192), new Point(144,192),
                new Point(144,193), new Point(145,193), new Point(145,194),
                new Point(146,194), new Point(146,195), new Point(147,195),
                new Point(147,196), new Point(148,196), new Point(149,196),
                new Point(150,196), new Point(151,196), new Point(151,195)
        ));
        ArrayList<Point> hidePath = path.getPathWithBarrierWithLimit(pos, target, leftPath, 30);
        for (Point point : hidePath) {
            System.out.println(point);
        }
    }
}
