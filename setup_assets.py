import os
import shutil
import wave
import math
import struct

base_dir = r"h:\9pazzle"
images_dir = os.path.join(base_dir, "images")
audio_dir = os.path.join(base_dir, "audio")

if not os.path.exists(audio_dir):
    os.makedirs(audio_dir)

# Copy images
source_dir = r"C:\Users\yabuk\.gemini\antigravity\brain\a882b4fc-6a3f-48aa-a4c6-2dfceed8d9da"
image_mapping = {
    "start_bg_1777113240746.png": "start-bg.png",
    "game_over_bg_1777113254823.png": "game-over-bg.png",
    "end_credit_bg_1777113269757.png": "end-credit-bg.png",
    "base_layer_1777113352154.png": "base-layer-1920x1080.png",
    "character_bg_1777113376699.png": "character-bg.png",
    "character_1777113441954.png": "character.png"
}

for src, dst in image_mapping.items():
    src_path = os.path.join(source_dir, src)
    dst_path = os.path.join(images_dir, dst)
    if os.path.exists(src_path):
        shutil.copy(src_path, dst_path)

# Copy character-rotate
char_src = os.path.join(images_dir, "character.png")
if os.path.exists(char_src):
    for i in range(1, 4):
        shutil.copy(char_src, os.path.join(images_dir, f"character-rotate-{i}.png"))

# Generate BGM wave
bgm_path = os.path.join(audio_dir, "bgm.wav")

def generate_bgm(filename, duration=60):
    sample_rate = 44100
    num_samples = int(sample_rate * duration)
    
    with wave.open(filename, 'w') as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        
        for i in range(num_samples):
            t = float(i) / sample_rate
            # Create a simple ambient, slightly eerie tone (e.g. pentatonic + drone)
            drone = math.sin(2.0 * math.pi * 55.0 * t) * 0.3
            melody_freq = 220.0 + 20.0 * math.sin(2.0 * math.pi * 0.2 * t)
            melody = math.sin(2.0 * math.pi * melody_freq * t) * 0.2
            
            value = drone + melody
            
            # envelope to avoid clipping and give some pulse
            envelope = 0.5 + 0.5 * math.sin(2.0 * math.pi * 0.5 * t)
            value = value * envelope
            
            # scale to 16-bit integer
            int_value = int(value * 32767.0)
            int_value = max(-32768, min(32767, int_value))
            
            data = struct.pack('<h', int_value)
            wav_file.writeframesraw(data)

generate_bgm(bgm_path)
print("Assets setup completed.")
