import json
import requests
from PIL import Image, ImageDraw

# Run inference on an image
url = "https://predict.ultralytics.com"
headers = {"x-api-key": "f26185fc36c0c08d7ea24ff1504bc91871f3c6d055"}
data = {"model": "https://hub.ultralytics.com/models/87xzKyH03feGafz4ZmTL", "imgsz": 640, "conf": 0.25, "iou": 0.45}
image_path = "download.png"

with open(image_path, "rb") as f:
    response = requests.post(url, headers=headers, data=data, files={"file": f})

# Check for successful response
response.raise_for_status()

# Parse inference results
results = response.json()

# Load the original image
image = Image.open(image_path)
draw = ImageDraw.Draw(image)

# Draw bounding boxes and labels
for result in results["images"][0]["results"]:
    box = result["box"]
    x1, y1, x2, y2 = box["x1"], box["y1"], box["x2"], box["y2"]
    label = result["name"]
    confidence = result["confidence"]

    # Draw rectangle
    draw.rectangle([x1, y1, x2, y2], outline="red", width=3)
    
    # Draw label
    draw.text((x1, y1), f"{label} {confidence:.2f}", fill="red")

# Save the image with bounding boxes
output_path = "output_bounding_boxes.png"
image.save(output_path)

# Print inference results
print(json.dumps(results, indent=2))
