package cn.wanyiwige.alexcavesspellsbook.client;

import cn.wanyiwige.alexcavesspellsbook.AlexCavesSpellsbook;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector3f;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** Loads the same closed triangle meshes exported for the nine Blockbench gems. */
public final class LustrousGemModels extends SimplePreparableReloadListener<List<LustrousGemModels.Mesh>> {
    private static volatile List<Mesh> meshes = List.of();
    public record Triangle(Vector3f a, Vector3f b, Vector3f c, Vector3f normal, int swatch) {}
    public record Mesh(ResourceLocation texture, List<Triangle> triangles, int trailColor) {}
    public static Mesh get(int kind) { return kind >= 0 && kind < meshes.size() ? meshes.get(kind) : null; }

    @Override
    protected List<Mesh> prepare(ResourceManager manager, ProfilerFiller profiler) {
        ResourceLocation source = new ResourceLocation(AlexCavesSpellsbook.MOD_ID, "models/projectile/lustrous_gems.json");
        try (Reader reader = manager.getResourceOrThrow(source).openAsReader()) {
            List<Mesh> result = new ArrayList<>();
            for (JsonElement element : JsonParser.parseReader(reader).getAsJsonArray()) {
                JsonObject gem = element.getAsJsonObject();
                List<Triangle> triangles = new ArrayList<>();
                for (JsonElement partElement : gem.getAsJsonArray("parts")) {
                    JsonObject part = partElement.getAsJsonObject();
                    List<Vector3f> vertices = new ArrayList<>();
                    for (JsonElement vertex : part.getAsJsonArray("vertices")) {
                        JsonArray xyz = vertex.getAsJsonArray();
                        vertices.add(new Vector3f(xyz.get(0).getAsFloat() / 16f,
                                xyz.get(1).getAsFloat() / 16f, xyz.get(2).getAsFloat() / 16f));
                    }
                    JsonArray faces = part.getAsJsonArray("faces");
                    JsonArray swatches = part.getAsJsonArray("swatches");
                    for (int i = 0; i < faces.size(); i++) {
                        JsonArray face = faces.get(i).getAsJsonArray();
                        Vector3f a = vertices.get(face.get(0).getAsInt());
                        Vector3f b = vertices.get(face.get(1).getAsInt());
                        Vector3f c = vertices.get(face.get(2).getAsInt());
                        Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
                        triangles.add(new Triangle(a, b, c, normal, swatches.get(i).getAsInt()));
                    }
                }
                result.add(new Mesh(new ResourceLocation(AlexCavesSpellsbook.MOD_ID,
                        "textures/entity/lustrous_gems/" + gem.get("file").getAsString() + ".png"), List.copyOf(triangles),
                        Integer.parseInt(gem.getAsJsonArray("palette").get(5).getAsString().substring(1), 16)));
            }
            if (result.size() != 9) throw new IllegalStateException("Expected nine lustrous gem meshes");
            return List.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load lustrous gem meshes: " + source, exception);
        }
    }

    @Override
    protected void apply(List<Mesh> prepared, ResourceManager manager, ProfilerFiller profiler) {
        meshes = prepared;
    }
}
