package ru.stankin.mj.view

import com.google.common.io.Files
import com.vaadin.cdi.CDIView
import com.vaadin.data.Container
import com.vaadin.data.Property
import com.vaadin.data.util.IndexedContainer
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent
import com.vaadin.server.*
import com.vaadin.shared.ui.label.ContentMode
import com.vaadin.ui.*
import org.apache.logging.log4j.LogManager
import org.apache.shiro.SecurityUtils
import org.vaadin.dialogs.ConfirmDialog
import org.vaadin.easyuploads.MultiFileUpload
import org.vaadin.easyuploads.UploadField
import ru.stankin.mj.model.*
import ru.stankin.mj.view.utils.*
import ru.stankin.mj.rested.security.MjRoles

import javax.inject.Inject
import java.io.*
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.Arrays
import java.util.concurrent.*


@CDIView("")
class MainView : CustomComponent(), View {


    @Inject
    private lateinit var userDao: UserResolver

    @Inject
    private lateinit var auth: AuthenticationsStore

    @Inject
    private lateinit var storage: StudentsStorage

    @Inject
    private lateinit var modules: ModulesStorage

    @Inject
    private lateinit var moduleJournalUploader: ModuleJournalUploader

    @Inject
    private lateinit var moduleJournalXMLUploader: ModulesJournalXMLUploader

    @Inject
    private lateinit var ecs: ExecutorService

    internal var marks: MarksTable? = null
    private var studentButtons: List<StudentButton>? = null
    private var studentLabel: Label? = null

    private val alarmHolder = AlarmHolder("Загрузка данных", this)
    private lateinit var semestrCbx: ComboBox

    val alarmsPanel = Panel()

    override fun enter(event: ViewChangeEvent) {

        addStyleName("main")
        logger.debug("entered")
        setHeight("100%")

        compositionRoot = VerticalLayout().apply {

            addComponent(Panel().apply {
                content = HorizontalLayout().apply {
                    setWidth("100%")

                    addComponent(Label("<b>&nbsp;МОДУЛЬНЫЙ ЖУРНАЛ</b>", ContentMode.HTML).apply {
                        setWidth(200f, Sizeable.Unit.PIXELS)
                    })

                    addComponent(createSemestrCbx())
                    addComponent(ratingRulesButton())

                    if (!SecurityUtils.getSubject().hasRole(MjRoles.ADMIN)) {
                        val studentRatingButton = StudentRatingButton()
                        addComponent(studentRatingButton)
                        val student = MjRoles.getUser() as Student?
                        studentRatingButton.setStudent(storage.getStudentById(student!!.id, currentSemester))
                    }

                    addComponentExpand(Label(""), 1f)

                    addComponentAlignment(kotlin.run {
                        val recoveryMode = SecurityUtils.getSubject().hasRole(MjRoles.PASSWORDRECOVERY)
                        val needChangePassword = recoveryMode || auth.acceptPassword(MjRoles.getUser()!!.id, MjRoles.getUser()!!.username)
                        val settings = Button("Аккаунт: " + MjRoles.getUser()!!.username) { event1 ->
                            openAccountWindow(recoveryMode)
                        }

                        if (needChangePassword) {
                            settings.click()
                        }
                        settings
                    }, Alignment.TOP_RIGHT)

                    addComponentAlignment(Button("Выход").apply {
                        addClickListener { event1 ->
                            SecurityUtils.getSubject().logout()
                            VaadinService.getCurrentRequest().wrappedSession.invalidate()
                            this.ui.page.reload()
                        }
                    }, Alignment.TOP_RIGHT)

                }
            }
            )

            addComponent(alarmsPanel.also {
                refreshAlarms()
            })

            addComponentExpand(kotlin.run {
                val mainPanel: Component
                if (SecurityUtils.getSubject().hasRole(MjRoles.ADMIN))
                    mainPanel = genUploadAndGrids()
                else {
                    mainPanel = genMarks()
                    val student = MjRoles.getUser() as Student?
                    setWorkingStudent(student!!.id, currentSemester!!)
                    //marks.fillMarks(storage.getStudentById(student.id, getCurrentSemester()));
                }
                mainPanel
            }, 1f)
            setSizeFull()
        }

    }

    private fun openAccountWindow(recoveryMode: Boolean) {
        if (SecurityUtils.getSubject().isAuthenticated) {
            val accountWindow = AccountWindow(MjRoles.getUser()!!, userDao, auth, false, !recoveryMode)
            accountWindow.addCloseListener { refreshAlarms() }
            this.ui.addWindow(accountWindow)
        } else
            this.ui.navigator.navigateTo("login")
    }

    private fun refreshAlarms() {
        alarmsPanel.content = VerticalLayout().apply {
            val user = MjRoles.getUser()!!
            if (!user.isAdmin && user.email.isNullOrEmpty())
                addComponent(Button("На вашем аккаунте не указан адрес электроной почты, пожалуйста, укажите его, " +
                        "он может пригодиться, например, для восстановления пароля.") { e ->
                    openAccountWindow(SecurityUtils.getSubject().hasRole(MjRoles.PASSWORDRECOVERY))
                }.apply {
//                    setIcon(ThemeResource("../runo/icons/32/folder.png"))
                    setStyleName("link v-Notification-warning");
                    setWidth(100f, Sizeable.Unit.PERCENTAGE);
                }
                )
        }
    }

    private fun createSemestrCbx(): ComboBox {
        semestrCbx = ComboBox()
        //semestrCbx.setContainerDataSource(new IndexedContainer(Arrays.asList("2014/2015 весна", "2014/2015 осень")));

        if (SecurityUtils.getSubject().hasRole(MjRoles.ADMIN)) {
            val semesters = ArrayList(storage.knownSemesters)
            semesters.add(ADD_SEMESTER_LABEL)
            val indexedContainer = IndexedContainer(semesters)
            semestrCbx.containerDataSource = indexedContainer
            semestrCbx.addValueChangeListener { event ->
                if (ADD_SEMESTER_LABEL == event.property.value) {
                    PromptDialog.prompt(this@MainView.ui, "Новый семестр", "Название") { text ->
                        if (text != null) {
                            indexedContainer.addItemAt(indexedContainer.size() - 1, text)
                            semestrCbx.select(text)
                        } else {
                            val size = semestrCbx.itemIds.size
                            if (size > 1)
                                semestrCbx.select(indexedContainer.itemIds[size - 2])
                        }
                    }
                } else {
                    setWorkingStudent(lastWorkingStudent, event.property.value as String)
                }
            }
            val size = semestrCbx.itemIds.size
            if (size > 1)
                semestrCbx.select(indexedContainer.itemIds[size - 2])
        } else {
            val student = MjRoles.getUser() as Student?
            semestrCbx.containerDataSource = IndexedContainer(storage.getStudentSemestersWithMarks(student!!.id))
            semestrCbx.addValueChangeListener { event -> setWorkingStudent(lastWorkingStudent, event.property.value as String) }
            val size = semestrCbx.itemIds.size
            if (size > 0)
                semestrCbx.select((semestrCbx.itemIds as List<*>)[size - 1])
        }

        //semestrCbx.setContainerDataSource(new IndexedContainer(Arrays.asList("2014/2015 весна", "2014/2015 осень")));

        semestrCbx.isTextInputAllowed = false
        semestrCbx.isNullSelectionAllowed = false
        return semestrCbx
    }

    private fun ratingRulesButton(): Button {
        return Button("Правила расчёта рейтинга") { event1 ->
            val window = Window("Правила расчёта рейтинга")
            val content1 = BrowserFrame("Правила расчёта рейтинга", ExternalResource("rating.html"))
            window.content = content1
            content1.setSizeFull()
            showCentralWindow(this.ui, window)
        }
    }

    private fun genUploadAndGrids(): HorizontalLayout {
        val uploadAndGrids = HorizontalLayout()
        //uploadAndGrids.setMargin(true);
        val uploads = VerticalLayout()
        uploads.setHeight(100f, Sizeable.Unit.PERCENTAGE)
        uploads.setMargin(true)
        uploads.isSpacing = true
        uploads.addComponent(createXMLUpload());
        uploads.addComponent(createEtalonUpload())
        uploads.addComponent(createMarksUpload())
        uploads.addComponent(Button("Удалить все модули") { event ->
            ConfirmDialog.show(this.ui, "Удаление всех модулей", "Вы уверены что хотите удалить все модули за "
                    + currentSemester + "-семестр ?" +
                    " Вам придется перезалить журналы, чтобы модули опять стали доступны",
                    "Удалить", "Отмена") { dialog ->
                if (dialog.isConfirmed) {
                    modules.deleteAllModules(currentSemester!!)
                    setWorkingStudent(null, currentSemester)
                }
            }
        })
        val c1 = Label()
        uploads.addComponent(c1)
        uploads.setExpandRatio(c1, 1f)
        val panel = Panel(uploads)
        panel.setWidth(200f, Sizeable.Unit.PIXELS)
        panel.setHeight(100f, Sizeable.Unit.PERCENTAGE)
        uploadAndGrids.addComponent(panel)

        val c = buildGrids()
        //TextArea c = new TextArea();
        c.setHeight(100f, Sizeable.Unit.PERCENTAGE)
        uploadAndGrids.addComponent(c)
        uploadAndGrids.setExpandRatio(c, 1f)
        uploadAndGrids.setSizeFull()
        return uploadAndGrids
    }

    private fun buildGrids(): Component {

        val students = Table()
        students.setWidth(100f, Sizeable.Unit.PERCENTAGE)
        students.setHeight(100f, Sizeable.Unit.PERCENTAGE)


        students.addContainerProperty("Группа", String::class.java, null)
        students.setColumnWidth("Группа", 60)
        students.addContainerProperty("Фамилия", String::class.java, null)
        students.setColumnWidth("Фамилия", 100)
        students.addContainerProperty("ИО", String::class.java, null)
        students.setColumnWidth("ИО", 30)
        students.addContainerProperty("Логин", String::class.java, null)
        students.setColumnWidth("Логин", 60)
        //        students.addContainerProperty("Пароль", String.class, null);
        //        students.setColumnWidth("Пароль", 60);

        students.isEditable = true
        students.isSelectable = true
        students.tableFieldFactory = object : TableFieldFactory {
            private val serialVersionUID = 1L

            override fun createField(container: Container, itemId: Any, propertyId: Any, uiContext: Component): Field<*>? {

                if (propertyId == "Пароль") {
                    val field = DefaultFieldFactory.get().createField(container, itemId, propertyId, uiContext)
                    field.addValueChangeListener { event ->
                        logger.debug("Property.ValueChangeEvent:" + event)
                        logger.debug("itemId:" + itemId)
                    }
                    return field
                } else
                    return null

            }
        }


        val searchField = TextField()
        searchField.setWidth(100f, Sizeable.Unit.PERCENTAGE)
        val searchLabel = Label("Поиск")
        searchLabel.setWidth(60f, Sizeable.Unit.PIXELS)
        val searchForm = HorizontalLayout(searchLabel, searchField)
        searchForm.setExpandRatio(searchField, 1f)
        searchForm.setWidth(100f, Sizeable.Unit.PERCENTAGE)
        val studentsContainer = StudentsContainer(storage)
        students.containerDataSource = studentsContainer

        searchField.addTextChangeListener { event1 ->
            val text = event1.text
            //if (text.length() > 2) {
            studentsContainer.filter = text
            //}

        }


        genMarks()

        studentLabel = Label("", ContentMode.HTML)
        studentLabel!!.setWidth(200f, Sizeable.Unit.PIXELS)
        studentButtons = Arrays.asList(
                StudentSettingsButton(),
                StudentRatingButton()
                //new StudentDeleteModulesButton()
        )
        val studentLine = HorizontalLayout(studentLabel)

        for (stbtn in studentButtons!!) {
            studentLine.addComponent(stbtn)
        }


        students.addValueChangeListener { event1 ->
            //logger.debug("selection:{}", event1);
            //logger.debug("stacktacer:{}",new Exception("stacktrace"));
            if (event1.property == null || event1.property.value == null)
                return@addValueChangeListener
            setWorkingStudent(event1.property.value as Int, currentSemester)

        }

        val grid = GridLayout(2, 3)
        grid.setHeight(100f, Sizeable.Unit.PERCENTAGE)
        grid.setWidth(100f, Sizeable.Unit.PERCENTAGE)
        //grid.addComponent(upload, 0, 0);
        grid.addComponent(searchForm, 0, 0)
        grid.addComponent(students, 0, 1)
        grid.addComponent(studentLine, 1, 0)
        grid.addComponent(marks, 1, 1)
        grid.isSpacing = true
        grid.setRowExpandRatio(0, 0f)
        grid.setRowExpandRatio(1, 1f)
        grid.setColumnExpandRatio(0, 2f)
        grid.setColumnExpandRatio(1, 1f)

        grid.setMargin(true)
        return /*new Panel(*/grid/*)*/
    }

    private var lastWorkingStudent: Int? = null

    private fun setWorkingStudent(studentId: Int?, currentSemester: String?) {
        var student: Student? = null
        if (studentId != null) {
            student = storage.getStudentById(studentId, currentSemester)
            if (studentLabel != null)
                studentLabel!!.value = "<b>" + student!!.surname + " " + student.initials + "</b>"
        } else {
            if (studentLabel != null)
                studentLabel!!.value = ""
        }

        if (marks != null)
            marks!!.fillMarks(student)

        if (studentButtons != null)
            for (stbtn in studentButtons!!) {
                stbtn.setStudent(student)
            }
        lastWorkingStudent = studentId
    }

    private fun genMarks(): Table {
        marks = MarksTable()
        marks!!.setSizeFull()
        marks!!.setWidth(100f, Sizeable.Unit.PERCENTAGE)
        return marks!!
    }

    private fun createEtalonUpload() = createSingleUpload("Загрузить эталон", "Эталон загружен: ", {
        moduleJournalUploader.updateStudentsFromExcel(currentSemester, it)
    })

    private fun createXMLUpload() = createSingleUpload("Загрузить XML", "XML загружен: ", {
        moduleJournalXMLUploader.updateFromXml(currentSemester!!, it)
    })

    private fun createSingleUpload(caption: String, completeMsg: String, uploader: (InputStream) -> List<String>): Component {
        val uploadField2 = object : UploadField() {
            override fun updateDisplay() {
                alarmHolder.post(completeMsg + this.lastFileName)
            }

        }

        uploadField2.fieldType = UploadField.FieldType.FILE
        uploadField2.caption = caption
        uploadField2.buttonCaption = "Выбрать файл"
        uploadField2.isFileDeletesAllowed = false

        uploadField2.addListener(Property.ValueChangeListener {
            val file = uploadField2.value as? File ?: return@ValueChangeListener

            if (checkWrongSemestr()) return@ValueChangeListener

            try {
                backupUpload(file, uploadField2.lastFileName)
                BufferedInputStream(FileInputStream(file)).use { bufferedInputStream ->
                    val log = uploader(bufferedInputStream)
                    alarmHolder.post(log.joinToString("\n"))
                    uploadField2.value = null
                }
            } catch (e: Exception) {
                alarmHolder.error(e)
            }
        })

        return uploadField2
    }


    private fun createMarksUpload(): MultiFileUpload {
        //verticalLayout.setMargin(true);
        //        FileReceiver uploadReceiver = new FileReceiver(this, moduleJournalUploader, ecs);
        //        Upload upload = new Upload("Загрузка файла", uploadReceiver);
        //
        //        uploadReceiver.serve(upload);

        val upload = object : MultiFileUpload() {

            override fun getAreaText(): String {
                return "<small>Перетащите<br/>файлы</small>"
            }

            override fun handleFile(file: File, fileName: String,
                                    mimeType: String, length: Long) {
                if (checkWrongSemestr()) return
                val msg = "Модульный журнал $fileName загружен"
                try {
                    logger.debug("uploading file {} {} at semester {}", fileName, file, currentSemester)
                    backupUpload(file, fileName)
                    val `is` = BufferedInputStream(FileInputStream(file))
                    val messages = moduleJournalUploader!!.updateMarksFromExcel(currentSemester, `is`)
                    messages.add(0, msg)
                    `is`.close()
                    val join = messages.joinToString("\n")
                    logger.debug("uploadmesages:{}", join)
                    alarmHolder.post(join)
                } catch (e: Exception) {
                    logger.error("error processing {}", file, e)
                    alarmHolder.error(e)
                }

            }

            //            @Override
            //            protected FileBuffer createReceiver() {
            //                FileBuffer receiver = super.createReceiver();
            //                /*
            //                 * Make receiver not to delete files after they have been
            //                 * handled by #handleFile().
            //                 */
            //                receiver.setDeleteFiles(false);
            //                return receiver;
            //            }
        }
        upload.caption = "Загрузить файлы с оценками"
        upload.uploadButtonCaption = "Выбрать файлы"
        upload.setRootDirectory(Files.createTempDir().toString())
        return upload
    }

    private val uploadsHistoryDir = Paths.get(System.getProperty("user.home"), "mjuploads")


    private fun backupUpload(f: File, fileName0: String?) {
        try {
            //TODO: на самом деле это не правильный способ, по-хорошему нужно заставить это делать MultiFileUpload и UploadField
            if (!java.nio.file.Files.exists(uploadsHistoryDir))
                java.nio.file.Files.createDirectories(uploadsHistoryDir)


            val origPath = f.toPath()
            val fileName = fileName0 ?: origPath.fileName.toString()

            val dotIndex = fileName.lastIndexOf('.')
            val extension = if (dotIndex == -1) "" else fileName.substring(dotIndex)
            val name = if (dotIndex == -1) fileName else fileName.substring(0, dotIndex)
            val formatter = DateTimeFormatter.ofPattern("-yyyy-MM-dd-HH-mm-ss-SSS")

            java.nio.file.Files.copy(origPath,
                    uploadsHistoryDir.resolve(name + formatter.format(LocalDateTime.now()) + extension)
            )


        } catch (e: Exception) {
            logger.warn("backup error:", e)
            e.printStackTrace()
        }

    }

    private fun checkWrongSemestr(): Boolean {
        if (currentSemester == null || currentSemester == ADD_SEMESTER_LABEL) {
            alarmHolder.post("Указано неверное название семестра")
            return true
        }
        return false
    }

    val currentSemester: String?
        get() = semestrCbx.value as? String

    private open inner class StudentButton(caption: String) : Button(caption) {
        protected var _student: Student? = null

        init {
            this.isEnabled = false
        }

        val student: Student
        get() = _student!!

        fun setStudent(student: Student?) {
            this.isEnabled = student != null
            this._student = student
        }
    }

    private inner class StudentSettingsButton : StudentButton("Редактировать") {
        init {
            this.addClickListener { event -> this.ui.addWindow(AccountWindow(student, userDao, auth, true)) }
        }

    }

    private inner class StudentRatingButton : StudentButton("Расcчитать рейтинг") {
        init {
            this.addClickListener { event ->
                this.setStudent(storage.getStudentById(this.student.id, currentSemester))
                val ratingCalculationTable = RatingCalculationTable(student)
                ratingCalculationTable.setSizeFull()
                val verticalLayout = VerticalLayout()
                verticalLayout.addComponent(ratingCalculationTable)
                verticalLayout.setExpandRatio(ratingCalculationTable, 1f)
                verticalLayout.addComponent(Label("Если вы хотите спрогнозировать свой рейтинг," + " вы можете ввести недостающие модули в этой форме"))
                verticalLayout.setSizeFull()
                val window = Window("Расчет рейтинга", verticalLayout)

                showCentralWindow(this.ui, window)
                Page.getCurrent().javaScript.execute("yaCounter29801259.hit('#calc');")
            }
        }

    }

    companion object {
        private val logger = LogManager.getLogger(MainView::class.java)
        private val ADD_SEMESTER_LABEL = "Добавить семестр"
    }
}

