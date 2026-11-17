import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.*;
import javax.imageio.ImageIO;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.client.renderer.texture.atlas.*;

/** Runs the installed 1.20.1 atlas JSON parser and sprite sources, without GL.
 * Args: vanilla extra.jar, mod.jar, expected number of discovered model textures.
 * The proxy represents a resource-pack stack, not an alternative atlas parser.
 */
public final class CandyStarcatcherAtlasCheck {
    static final String NS = "alex_caves_spellsbook";
    static final Map<ResourceLocation,List<Resource>> resources = new LinkedHashMap<>();
    static final Set<ResourceLocation> expected = new TreeSet<>(Comparator.comparing(Object::toString));

    public static void main(String[] args) throws Exception {
        try (var vanilla = new ZipFile(args[0]); var mod = new ZipFile(args[1])) {
            addPack(vanilla); addPack(mod);
            for (var entry : Collections.list(mod.entries())) {
                String name = entry.getName();
                if (!(name.startsWith("assets/"+NS+"/models/item/candy_starcatcher")) || !name.endsWith(".json")) continue;
                var model = JsonParser.parseString(new String(mod.getInputStream(entry).readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
                if (!model.has("textures")) continue;
                for (var value : model.getAsJsonObject("textures").entrySet()) {
                    String id = value.getValue().getAsString();
                    if (id.startsWith(NS+":item/candy_starcatcher")) expected.add(new ResourceLocation(id));
                }
            }
            ResourceManager manager = (ResourceManager) Proxy.newProxyInstance(CandyStarcatcherAtlasCheck.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(proxy,method,a)-> {
                return switch (method.getName()) {
                    case "getResourceStack" -> resources.getOrDefault(a[0],List.of());
                    case "getResource" -> {var stack=resources.get(a[0]); yield stack==null?Optional.empty():Optional.of(stack.get(stack.size()-1));}
                    case "listResources" -> {
                        var result = new LinkedHashMap<ResourceLocation,Resource>();
                        @SuppressWarnings("unchecked") var filter=(Predicate<ResourceLocation>)a[1];
                        for (var row:resources.entrySet()) {
                            String path=row.getKey().toString().split(":",2)[1];
                            if(path.startsWith(a[0]+"/") && filter.test(row.getKey())) result.put(row.getKey(),row.getValue().get(row.getValue().size()-1));
                        }
                        yield result;
                    }
                    case "toString" -> "ZipResourcePackStack";
                    default -> throw new UnsupportedOperationException(method.toString());
                };
            });
            var loader = SpriteResourceLoader.load(manager,new ResourceLocation("minecraft:blocks"));
            var field=Arrays.stream(SpriteResourceLoader.class.getDeclaredFields()).filter(f->f.getType()==List.class).findFirst().orElseThrow(); field.setAccessible(true);
            @SuppressWarnings("unchecked") var sources=(List<SpriteSource>)field.get(loader);
            var found = new HashSet<ResourceLocation>();
            var output = (SpriteSource.Output) Proxy.newProxyInstance(CandyStarcatcherAtlasCheck.class.getClassLoader(),new Class<?>[]{SpriteSource.Output.class},(proxy,method,a)-> {
                switch(method.getName()) {
                    case "add" -> {
                        ResourceLocation id=(ResourceLocation)a[0]; found.add(id);
                        if(expected.contains(id)) {
                            try(var input=((Resource)a[1]).open()) {
                                var image=ImageIO.read(input);
                                if(image==null || image.getWidth()==0) throw new AssertionError("Invalid PNG: "+id);
                                System.out.println("ATLAS_PNG "+id+" "+image.getWidth()+"x"+image.getHeight());
                            }
                        }
                    }
                    
                    case "removeAll" -> {
                        @SuppressWarnings("unchecked") var predicate=(Predicate<ResourceLocation>)a[0]; found.removeIf(predicate);
                    }
                    default -> throw new UnsupportedOperationException(method.toString());
                }
                return null;
            });
            for(var source:sources) source.run(manager,output);
            var missing=new TreeSet<>(expected); missing.removeAll(found);
            int discovered=expected.size()-missing.size();
            System.out.println("ATLAS_DISCOVERY expected="+expected.size()+" discovered="+discovered+" missing="+missing.size()+" sources="+sources.size());
            if(discovered!=Integer.parseInt(args[2])) throw new AssertionError("Unexpected atlas coverage: "+missing);
            if(!found.contains(new ResourceLocation("minecraft:item/diamond"))) throw new AssertionError("Vanilla sources lost");
            System.out.println("PASS ATLAS_DISCOVERY; vanilla item/diamond retained; GPU rendering not tested");
        }
    }

    static void addPack(ZipFile zip) throws IOException {
        for(var entry:Collections.list(zip.entries())) {
            String[] path=entry.getName().split("/",3);
            if(entry.isDirectory() || path.length!=3 || !path[0].equals("assets")) continue;
            var id=new ResourceLocation(path[1],path[2]);
            var resource=new Resource(null,()->zip.getInputStream(entry));
            resources.computeIfAbsent(id,key->new ArrayList<>()).add(resource);
        }
    }
}
