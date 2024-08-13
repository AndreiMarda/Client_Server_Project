package NewPack;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.io.IOException;

public class Monitor implements Runnable {
    private Path dirToWatch;

    public Monitor(String dirPath) {
        this.dirToWatch = Paths.get(dirPath);
    }

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            dirToWatch.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            while (true) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException x) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
					WatchEvent<Path> ev = (WatchEvent<Path>)event;
                    Path filename = ev.context();
                    Path fullPath = dirToWatch.resolve(filename);

                    System.out.println(kind.name() + ": " + filename);
                    
                    if (kind == ENTRY_CREATE) {
                        System.out.println("Handling file creation: " + fullPath);
                    
                    } else if(kind == ENTRY_MODIFY) {
                        System.out.println("Handling file modification: " + fullPath);
                        
                    } else if(kind == ENTRY_DELETE) {
                    	System.out.println("Handling file or folder deletion");
	
                    }
                    
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (IOException x) {
            System.out.println("IOException: " + x);
        }
    }
}
