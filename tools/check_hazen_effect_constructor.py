"""Exercise the actual client callback bodies with a constructor-calling parent.

This is a narrow Java lifecycle regression, not a Minecraft client launch.
"""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
JAVA = Path('C:/Program Files/Microsoft/jdk-17.0.15.6-hotspot/bin')
OUT = ROOT / 'outputs/hazen-effect-constructor-check'
OUT.mkdir(parents=True, exist_ok=True)


def block(text, marker):
    start = text.index('{', text.index(marker))
    depth = 1
    end = start + 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return text[start:end]


def check(source, fixed):
    callback = block(source, 'public void initializeClient(')
    callback = callback.replace('net.minecraftforge.client.extensions.common.IClientMobEffectExtensions', 'Extension')
    effect_callback = '' if fixed else 'public void initializeClient(java.util.function.Consumer<Extension> consumer)' + callback
    soul = ('static class SoulFireEffect extends Effect { SoulFireEffect(){super("soul_fire");}'
            'public void initializeClient(java.util.function.Consumer<Extension> consumer)' + callback + '}') if fixed else ''
    code = '''import java.util.function.Consumer;
public class ConstructorCheck {
  static class MobEffectInstance {}
  interface Extension { default boolean isVisibleInInventory(MobEffectInstance instance){return true;} }
  static class Parent {
    Extension extension;
    Parent(){initializeClient(value -> extension=value);}
    public void initializeClient(Consumer<Extension> consumer){}
  }
  static class Effect extends Parent {
    final String kind;
    Effect(String kind){this.kind=kind;}
    CALLBACK
  }
  SOUL
  public static void main(String[] args){
    for(String name:new String[]{"flame_kissed","moonkissed","ichor","infested","spores","electrocuted","hexed","soul_fire","gluttonous_hunger"}){
      Effect effect=FACTORY;
      boolean visible=effect.extension==null || effect.extension.isVisibleInInventory(new MobEffectInstance());
      if(visible==name.equals("soul_fire"))throw new AssertionError(name);
    }
    System.out.println("PASS: nine effects construct with early client callback; only soul fire hides inventory icon");
  }
}
'''.replace('CALLBACK', effect_callback).replace('SOUL', soul).replace('FACTORY',
    'name.equals("soul_fire")?new SoulFireEffect():new Effect(name)' if fixed else 'new Effect(name)')
    path = OUT / 'ConstructorCheck.java'
    path.write_text(code, encoding='utf-8')
    subprocess.run([str(JAVA/'javac.exe'), '-d', str(OUT), str(path)], check=True)
    run = subprocess.run([str(JAVA/'java.exe'), '-cp', str(OUT), 'ConstructorCheck'], capture_output=True, text=True)
    if fixed:
        assert run.returncode == 0, run.stderr
        print(run.stdout.strip())
    else:
        assert run.returncode != 0 and 'NullPointerException' in run.stderr, run.stderr
        print('PASS: preview.6 callback reproduces constructor NullPointerException')


# Recreate the failing callback shown in the crash report as a negative control.
current = (ROOT/'src/main/java/cn/wanyiwige/alexcavesspellsbook/hazen/HazenSecondaryEffects.java').read_text(encoding='utf-8')
old_callback = current.replace('consumer.accept(new net.minecraftforge.client.extensions.common.IClientMobEffectExtensions(){',
                               'if(kind.equals("soul_fire"))consumer.accept(new net.minecraftforge.client.extensions.common.IClientMobEffectExtensions(){', 1)
check(old_callback, False)
check(current, True)
