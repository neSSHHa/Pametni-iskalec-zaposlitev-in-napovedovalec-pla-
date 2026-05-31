FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py train_model.py ./
COPY tests ./tests

EXPOSE 8091

CMD ["sh", "-c", "if [ ! -f models/salary_model.joblib ]; then python train_model.py || echo 'Salary model training skipped; predictions will remain unavailable until training succeeds.'; fi; exec uvicorn app:app --host 0.0.0.0 --port 8091"]
