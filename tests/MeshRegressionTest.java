import com.visionmod.VisionGeometry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
/** CPU vertex sink tests; these do not emulate a GPU or shader compatibility. */
public class MeshRegressionTest {
    static class Sink implements VertexConsumer {
        final List<float[]> vertices=new ArrayList<>();
        public VertexConsumer addVertex(PoseStack.Pose p,float x,float y,float z){vertices.add(new float[]{x,y,z});return this;}
        public VertexConsumer setColor(int c){return this;}
        public VertexConsumer setUv(float u,float v){if(!Float.isFinite(u)||!Float.isFinite(v))throw new AssertionError("UV");return this;}
        public VertexConsumer setOverlay(int o){if(o!=655360)throw new AssertionError("overlay");return this;}
        public VertexConsumer setLight(int l){return this;}
        public VertexConsumer setNormal(PoseStack.Pose p,float x,float y,float z){if(!Float.isFinite(x+y+z))throw new AssertionError("normal");return this;}
        public VertexConsumer setLineWidth(float w){return this;}
    }
    static double area(float[] a,float[] b,float[] c){
        double ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
        return Math.sqrt(Math.pow(uy*vz-uz*vy,2)+Math.pow(uz*vx-ux*vz,2)+Math.pow(ux*vy-uy*vx,2));
    }
    public static void main(String[] args){
        var pose=new PoseStack();var sphere=new Sink();
        VisionGeometry.ellipsoid(sphere,pose,1,2,3,.2f,.3f,.4f,0xFFABCDEF,14,8);
        if(sphere.vertices.size()!=14*8*4)throw new AssertionError("sphere topology");
        for(var v:sphere.vertices)if(!Float.isFinite(v[0]+v[1]+v[2])||Math.abs(v[0]-1)>.20001||Math.abs(v[1]-2)>.30001||Math.abs(v[2]-3)>.40001)throw new AssertionError("sphere bounds");
        for(int i=0;i<sphere.vertices.size();i+=4)if(area(sphere.vertices.get(i),sphere.vertices.get(i+1),sphere.vertices.get(i+2))+area(sphere.vertices.get(i),sphere.vertices.get(i+2),sphere.vertices.get(i+3))<1e-8)throw new AssertionError("degenerate sphere quad");
        var cached=new Sink();VisionGeometry.ellipsoid(cached,pose,1,2,3,.2f,.3f,.4f,0xFFABCDEF,14,8);
        for(int i=0;i<cached.vertices.size();i++)if(!Arrays.equals(cached.vertices.get(i),sphere.vertices.get(i)))throw new AssertionError("cache changed mesh");
        var band=new Sink();VisionGeometry.band(band,pose,0,0,0,.6,.05,0xAAFFFFFF,0x00FFFFFF,28);
        if(band.vertices.size()!=28*8)throw new AssertionError("two-sided band");
        for(int i=0;i<band.vertices.size();i+=4)if(area(band.vertices.get(i),band.vertices.get(i+1),band.vertices.get(i+2))<1e-8)throw new AssertionError("degenerate band");
        var lines=new Sink();VisionGeometry.line(lines,pose,1,1,1,1,1,1,0xFFFFFFFF,1);if(!lines.vertices.isEmpty())throw new AssertionError("zero length line");
        VisionGeometry.line(lines,pose,1,1,1,2,3,4,0xFFFFFFFF,1);if(lines.vertices.size()!=2)throw new AssertionError("line endpoints");
        System.out.println("PASS: sphere/cache/band/line mesh checks (CPU vertex sink)");
    }
}
