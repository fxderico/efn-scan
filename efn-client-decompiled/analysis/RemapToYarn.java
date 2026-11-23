import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

import java.nio.file.Path;

/**
 * Remaps the mod jar intermediary -> Yarn "named", including Mixin annotation strings,
 * @Shadow members and overrides of Minecraft methods (needs the intermediary MC jar on
 * the classpath for the class hierarchy).
 * Usage: java -cp tiny-remapper-fat.jar RemapToYarn.java <in.jar> <out.jar> <yarn.tiny> <classpath.jar>...
 */
public class RemapToYarn {
    public static void main(String[] a) throws Exception {
        Path in = Path.of(a[0]), out = Path.of(a[1]), mappings = Path.of(a[2]);
        TinyRemapper r = TinyRemapper.newRemapper()
                .withMappings(TinyUtils.createTinyMappingProvider(mappings, "intermediary", "named"))
                .ignoreConflicts(true)
                .renameInvalidLocals(true)
                .extension(new MixinExtension())
                .build();
        try (OutputConsumerPath consumer = new OutputConsumerPath.Builder(out).build()) {
            consumer.addNonClassFiles(in, NonClassCopyMode.FIX_META_INF, r);
            r.readInputs(in);
            for (int i = 3; i < a.length; i++) r.readClassPath(Path.of(a[i]));
            r.apply(consumer);
        } finally {
            r.finish();
        }
    }
}
