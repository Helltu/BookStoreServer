import os

# Настройки
PROJECT_ROOT = '.'  # Текущая папка
OUTPUT_FILE = 'project_context.txt'

# Расширения файлов, которые нужно включить
# Добавьте или удалите ненужные (например, .html, .js, если есть фронт)
INCLUDED_EXTENSIONS = {
    '.java',
    '.xml',       # pom.xml, настройки
    '.yml',       # application.yml
    '.yaml',
    '.properties', # application.properties
    '.gradle',    # build.gradle
    '.sql',       # скрипты миграций
    '.md'         # README
}

# Папки, которые нужно ИГНОРИРОВАТЬ
EXCLUDED_DIRS = {
    '.git',
    '.idea',
    '.vscode',
    'target',
    'build',
    'gradle',
    'wrapper',
    'node_modules',
    '.mvn'
}

def collect_project_files():
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as outfile:
        # Проходим по всем папкам
        for root, dirs, files in os.walk(PROJECT_ROOT):
            # Модифицируем dirs in-place, чтобы os.walk не заходил в исключенные папки
            dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]

            for file in files:
                # Проверяем расширение файла
                if any(file.endswith(ext) for ext in INCLUDED_EXTENSIONS):
                    file_path = os.path.join(root, file)

                    # Получаем относительный путь для красивого заголовка
                    relative_path = os.path.relpath(file_path, PROJECT_ROOT)

                    # Исключаем сам скрипт и файл вывода, если они попали под фильтр
                    if file == 'collect_code.py' or file == OUTPUT_FILE:
                        continue

                    print(f"Обработка: {relative_path}")

                    try:
                        # Записываем заголовок
                        outfile.write(f"\n{'='*60}\n")
                        outfile.write(f"FILE PATH: {relative_path}\n")
                        outfile.write(f"{'='*60}\n\n")

                        # Записываем содержимое
                        with open(file_path, 'r', encoding='utf-8') as infile:
                            outfile.write(infile.read())

                        outfile.write("\n") # Отступ после файла
                    except Exception as e:
                        print(f"Ошибка чтения файла {relative_path}: {e}")

    print(f"\nГотово! Весь код собран в файл: {OUTPUT_FILE}")

if __name__ == '__main__':
    collect_project_files()