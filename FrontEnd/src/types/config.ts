// Placeholder for ManageablePropertyDto
export interface ManageablePropertyDto {
  key: string;
  currentValue: string | null;
  defaultValue: string;
  description: string;
  // Add other relevant fields if known
}

// Placeholder for UpdatePropertyRequestDto
export interface UpdatePropertyRequestDto {
  key: string;
  value: string | null; // Null value resets to default
}

// Add other config-related types if needed
