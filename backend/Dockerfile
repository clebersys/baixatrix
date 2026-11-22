FROM denoland/deno:bin-2.6.10 AS deno-runtime

FROM python:3.12-slim

WORKDIR /app

# ffmpeg ajuda o yt-dlp a lidar com alguns formatos; é opcional, mas leve.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*

COPY --from=deno-runtime /deno /usr/local/bin/deno

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
