import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.PathImpl;
import com.huawei.codecraft.way.SimplePathImpl;
import org.junit.Test;

import java.util.ArrayList;

public class PathTest2 {

    @Test
    public void test01(){
        char[][] inputmap = new char[][] {
                "....................................................................................................".toCharArray(),
                "....................................................................................................".toCharArray(),
                "....................BBBB........................................BBBB.....................BBBB........".toCharArray(),
                "....................BBBB........................................BBBB.....................BBBB........".toCharArray(),
                "..................BBBB........#.#.#....................#.#........BBBB........#.#.#........BBBB......".toCharArray(),
                // Continue filling in rows based on the pattern you've provided.
                // Replace '*' with '.' in the rest of your pattern.
                "....#.......#.................#.......#.....#.#.....#.......#.................#.......#.....#.#......".toCharArray(),
                "....#.#.#....................#.#.#....................#.#.#....................#.#.#................".toCharArray(),
                "....#.#.#.#..................##.##.#..................##.##.#..................#.##.#...............".toCharArray(),
                "....#.#.#.#.................#.#.#.#.................#.#.#.#.................#.#.#.#................".toCharArray(),
                // Fill every line as necessary up to 100.
                "....................................................................................................".toCharArray(),
                "....................................................................................................".toCharArray(),
        };

        Path path = new PathImpl();
        Mapinfo.init(inputmap);

        ArrayList<Point> path1 = path.getPath(new Point(7, 14), new Point(3, 25));
        ArrayList<Point> path2 = path.getPath(new Point(1, 3), new Point(6, 9));
        System.out.println(path1);
        System.out.println(path2);
    }

}
