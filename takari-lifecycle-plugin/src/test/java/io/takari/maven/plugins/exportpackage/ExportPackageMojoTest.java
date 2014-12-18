package io.takari.maven.plugins.exportpackage;

import static org.apache.maven.plugin.testing.resources.TestResources.rm;
import static org.apache.maven.plugin.testing.resources.TestResources.touch;
import io.takari.incrementalbuild.maven.testing.IncrementalBuildRule;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class ExportPackageMojoTest {

  @Rule
  public final TestResources resources = new TestResources();

  @Rule
  public final IncrementalBuildRule mojos = new IncrementalBuildRule();

  @Test
  public void testBasic() throws Exception {
    File basedir = resources.getBasedir("jar/project-with-resources");

    // initial build
    mkfile(basedir, "target/classes/exported/Exported.class");
    mkfile(basedir, "target/classes/internal/Internal.class");
    mojos.executeMojo(basedir, "export-package");
    mojos.assertBuildOutputs(basedir, "target/classes/" + ExportPackageMojo.PATH_EXPORT_PACKAGE);
    assertExportedPackages(basedir, "exported");

    // no-change rebuild
    mojos.executeMojo(basedir, "export-package");
    mojos.assertCarriedOverOutputs(basedir, "target/classes/" + ExportPackageMojo.PATH_EXPORT_PACKAGE);
    assertExportedPackages(basedir, "exported");

    // change public and private classes
    touch(basedir, "target/classes/exported/Exported.class");
    touch(basedir, "target/classes/internal/Internal.class");
    mojos.executeMojo(basedir, "export-package");
    mojos.assertCarriedOverOutputs(basedir, "target/classes/" + ExportPackageMojo.PATH_EXPORT_PACKAGE);
    assertExportedPackages(basedir, "exported");

    // remove private class
    rm(basedir, "target/classes/internal/Internal.class");
    mojos.executeMojo(basedir, "export-package");
    mojos.assertCarriedOverOutputs(basedir, "target/classes/" + ExportPackageMojo.PATH_EXPORT_PACKAGE);
    assertExportedPackages(basedir, "exported");

    // new public class
    mkfile(basedir, "target/classes/exported/another/Exported.class");
    mojos.executeMojo(basedir, "export-package");
    mojos.assertBuildOutputs(basedir, "target/classes/" + ExportPackageMojo.PATH_EXPORT_PACKAGE);
    assertExportedPackages(basedir, "exported", "exported.another");

    // remove public class
    rm(basedir, "target/classes/exported/another/Exported.class");
    mojos.executeMojo(basedir, "export-package");
    mojos.assertBuildOutputs(basedir, "target/classes/" + ExportPackageMojo.PATH_EXPORT_PACKAGE);
    assertExportedPackages(basedir, "exported");

    // remove last public class
    rm(basedir, "target/classes/exported/Exported.class");
    mojos.executeMojo(basedir, "export-package");
    mojos.assertBuildOutputs(basedir, "target/classes/" + ExportPackageMojo.PATH_EXPORT_PACKAGE);
    assertExportedPackages(basedir, new String[0]);
  }

  private void assertExportedPackages(File basedir, String... exportedPackages) throws IOException {
    List<String> actual = Files.readLines(new File(basedir, "target/classes/" + ExportPackageMojo.PATH_EXPORT_PACKAGE), Charsets.UTF_8);
    Assert.assertEquals(toString(Arrays.asList(exportedPackages)), toString(actual));
  }

  private String toString(Collection<String> strings) {
    StringBuilder sb = new StringBuilder();
    for (String string : new TreeSet<>(strings)) {
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(string);
    }
    return sb.toString();
  }

  private void mkfile(File basedir, String relpath) throws IOException {
    File file = new File(basedir, relpath);
    file.getParentFile().mkdirs();
    file.createNewFile();
  }
}