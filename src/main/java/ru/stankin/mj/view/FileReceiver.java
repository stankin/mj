package ru.stankin.mj.view;

import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Upload;
import ru.stankin.mj.model.ModuleJournalUploader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.concurrent.*;

/**
* Created by nickl on 31.01.15.
*/
class FileReceiver implements Upload.Receiver {

    private Future<String> parsing = null;

    private Upload upload;
    private MainView components;
    private ExecutorService ecs;
    private ModuleJournalUploader moduleJournalUploader;

    public FileReceiver(MainView components, ModuleJournalUploader moduleJournalUploader, ExecutorService ecs) {
        this.components = components;
        this.ecs = ecs;
        this.moduleJournalUploader = moduleJournalUploader;
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        parsing = null;
        PipedInputStream pipedInputStream = new PipedInputStream(1024 * 100);
        try {
            parsing = ecs.submit(() -> {
                try {
                    List<String> messages = moduleJournalUploader.updateMarksFromExcel(pipedInputStream);
                    parsing = null;
                    return String.join("\n", messages);
                } catch (Exception e) {
                    try {
                        pipedInputStream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    return e.getMessage();
                }
            });
            return new PipedOutputStream(pipedInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//            try {
//                return filesStorage.createNew(filename);
//            } catch (FileNotFoundException e) {
//                throw new RuntimeException(e);
//            }
    }

    public void serve(Upload upload) {
        this.upload = upload;

        upload.addSucceededListener(
                event1 -> showUploadSuccess(event1, parsing)
        );

        upload.addFailedListener(e -> {
            showUploadError(parsing);
        });


    }

    private void showUploadSuccess(Upload.SucceededEvent event1, Future<String> future) {
        if (getStoredError(future) == null) {
            Notification notification = new Notification("Файл " + event1.getFilename() + " загружен",
                    Notification.Type
                    .TRAY_NOTIFICATION);
            notification.setPosition(Position.BOTTOM_LEFT);
            notification.show(components.getUI().getPage());
        }
        else
            showUploadError(future);
    }

    private void showUploadError(Future<String> future) {
        String storedError = getStoredError(future);
        Notification.show(
                "Ошибка загрузки файла",
                storedError,
                Notification.Type.ERROR_MESSAGE);
    }

    private String getStoredError(Future<String> parsing) {
        if (parsing == null)
            return null;
        try {
            return parsing.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return e.getMessage();
        } catch (ExecutionException e) {
            return e.getMessage();
        } catch (TimeoutException e) {
            return e.getMessage();
        }

    }
}
