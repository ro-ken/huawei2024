import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.PathImpl;
import com.huawei.codecraft.way.RegionManager;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.huawei.codecraft.Const.berths;
import static com.huawei.codecraft.Const.pointToBerth;

public class PathTest2 {

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

    private void init() {
       for (int i = 0; i < 10; i++) {
           berths[i] = new Berth(i);
       }
        berths[0].pos.x = 2;
        berths[0].pos.y = 112;

        berths[1].pos.x = 2;
        berths[1].pos.y = 160;

        berths[2].pos.x = 2;
        berths[2].pos.y = 187;

        berths[3].pos.x = 19;
        berths[3].pos.y = 86;

        berths[4].pos.x = 28;
        berths[4].pos.y = 72;

        berths[5].pos.x = 31;
        berths[5].pos.y = 61;

        berths[6].pos.x = 37;
        berths[6].pos.y = 41;

        berths[7].pos.x = 116;
        berths[7].pos.y = 2;

        berths[8].pos.x = 134;
        berths[8].pos.y = 2;

        berths[9].pos.x = 171;
        berths[9].pos.y = 2;

        for (int i = 0; i < berths.length; i++) {
            pointToBerth.put(berths[i].pos, berths[i]);
        }
    }

    @Test
    public void teset_Class_RegionManager() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("test\\map1.txt"));
            char[][] inputmap = new char[lines.size()][];
            for (int i = 0; i < lines.size(); i++) {
//                System.out.println(lines.get(i).toCharArray());
                inputmap[i] = lines.get(i).toCharArray();
            }

            long startTime = System.nanoTime();  // Start timing
            Path path = new PathImpl();
            Mapinfo.init(inputmap);
            init(); // 单元测试需要自己手动输入berth信息
            RegionManager regionManager = new RegionManager(path);

            System.out.println("*************initial initialization **************");
            regionManager.createRegions();
            regionManager.printRegionDetails();
            System.out.println("*********************************************");

            System.out.println("*************get full Path**************");
            regionManager.getFullPath();
            regionManager.printBerthToPointPathsDetails();
            regionManager.printPointToBerthPathsDetails();
            System.out.println("*********************************************");

            System.out.println("*************second splitRegion**************");
            regionManager.splitRegions();
//            regionManager.printPointDetails();
            System.out.println("*********************************************");

            long endTime = System.nanoTime();  // End timing
            System.out.println("region init Time taken: " + (endTime - startTime) + " ns");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
