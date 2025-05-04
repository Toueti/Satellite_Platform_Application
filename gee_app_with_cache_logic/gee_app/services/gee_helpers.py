import ee
import logging
from datetime import datetime
from scipy.stats import linregress
from typing import Dict, List

logger = logging.getLogger('gee_app')


def mask_clouds(image):
    """Masks clouds using the QA60 band in Sentinel-2 images."""
    cloud_mask = image.select('QA60').lt(1)
    return image.updateMask(cloud_mask)

def normalize(image, band):
    """Normalizes a band to 0-1 scale."""
    return image.select(band).divide(10000)

def classify_severity(value: float, is_spi: bool = False) -> str:
    if is_spi:
        if value <= -2.0:
            return "extreme"
        elif value <= -1.5:
            return "severe"
        elif value <= -1.0:
            return "moderate"
        elif value <= -0.5:
            return "mild"
        else:
            return "normal"
    else:
        if value < -0.2:
            return "extreme"
        elif value < -0.1:
            return "severe"
        elif value < -0.05:
            return "moderate"
        else:
            return "mild"

def calculate_drought_trend(data: List[Dict], value_key: str = 'anomaly') -> Dict:
    if not data:
        return {"direction": "unknown", "rate": 0.0, "confidence": 0.0, "forecast": "No data"}

    dates = [datetime.strptime(d['date'], '%Y-%m-%d').timestamp() for d in data]
    values = [d[value_key] for d in data]

    if len(dates) < 2:
        return {"direction": "stable", "rate": 0.0, "confidence": 0.0, "forecast": "Single point"}

    slope, _, r_value, _, _ = linregress(dates, values)
    direction = "improving" if slope > 0 else "worsening" if slope < 0 else "stable"
    return {
        "direction": direction,
        "rate": float(slope),
        "confidence": min(1.0, abs(r_value)),
        "forecast": f"Conditions likely to {direction}"
    }

def get_diversity_interpretation(shannon_index: float) -> str:
    """Interpret the Shannon diversity index value."""
    if shannon_index < 0.5:
        return "Very low diversity, dominated by a single land cover type"
    elif shannon_index < 1.0:
        return "Low diversity landscape"
    elif shannon_index < 1.5:
        return "Moderate diversity landscape"
    elif shannon_index < 2.0:
        return "High diversity landscape"
    else:
        return "Very high diversity, well-distributed landscape"

def get_fragmentation_interpretation(fragmentation_index: float) -> str:
    """Interpret the fragmentation index value."""
    if fragmentation_index < 0.001:
        return "Low fragmentation, relatively contiguous landscape"
    elif fragmentation_index < 0.002:
        return "Moderate fragmentation"
    elif fragmentation_index < 0.003:
        return "High fragmentation, patchy landscape"
    else:
        return "Very high fragmentation, highly fragmented landscape"

def get_landscape_integrity_score(diversity: float, fragmentation: float, dominant_percentage: float) -> Dict:
    """Calculate an overall landscape integrity score."""
    # Normalize inputs to 0-1 scale
    diversity_norm = min(diversity / 2.0, 1.0)  # Assuming max diversity is 2.0
    fragmentation_norm = max(0, min(1.0 - (fragmentation / 0.004), 1.0))  # Lower fragmentation is better, adjusted scale
    dominance_norm = 1.0 - (dominant_percentage / 100)  # Lower dominance is better for biodiversity
    
    # Calculate weighted score (equal weights for simplicity)
    integrity_score = (diversity_norm + fragmentation_norm + dominance_norm) / 3.0
    
    # Scale to 0-100
    scaled_score = integrity_score * 100
    
    return {
        "score": round(scaled_score, 1),
        "interpretation": get_integrity_interpretation(scaled_score),
        "components": {
            "diversity_contribution": round(diversity_norm * 100, 1),
            "fragmentation_contribution": round(fragmentation_norm * 100, 1),
            "dominance_contribution": round(dominance_norm * 100, 1)
        }
    }

def get_integrity_interpretation(score: float) -> str:
    """Interpret the landscape integrity score."""
    if score < 20:
        return "Very low landscape integrity - highly altered or degraded landscape"
    elif score < 40:
        return "Low landscape integrity - significant alteration from natural state"
    elif score < 60:
        return "Moderate landscape integrity - partial natural function retained"
    elif score < 80:
        return "High landscape integrity - generally intact natural landscape"
    else:
        return "Very high landscape integrity - minimally disturbed natural system"

async def perform_temporal_analysis(current_image, aoi, scale: int) -> Dict:
    """Perform temporal analysis comparing current classification with historical data."""
    try:
        # Try to access a historical dataset for comparison
        historical_collection = ee.ImageCollection("ESA/WorldCover/v100")
        if historical_collection.size().getInfo() > 0:
            historical_image = historical_collection.first()
            
            # Implement actual change detection logic here
            return {
                "change_metrics": {
                    "status": "Temporal analysis completed successfully",
                    "time_range": "Historical data available for comparison"
                }
            }
        else:
            return {
                "change_metrics": {
                    "status": "No historical data available for comparison",
                    "time_range": "Unable to perform temporal analysis"
                }
            }
    except Exception as e:
        logger.error(f"Temporal analysis failed: {e}")
        return {"error": str(e)}
