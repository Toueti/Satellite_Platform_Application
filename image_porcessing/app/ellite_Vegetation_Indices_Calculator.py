import os
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.colors import LinearSegmentedColormap
import rasterio
from rasterio.plot import show
import tkinter as tk
from tkinter import filedialog, ttk, messagebox
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg


class VegetationIndicesCalculator:
    def __init__(self, master):
        self.master = master
        master.title("Satellite Vegetation Indices Calculator")
        master.geometry("1200x800")
        
        # Variables
        self.filepath = tk.StringVar()
        self.band_red = tk.StringVar(value="1")
        self.band_nir = tk.StringVar(value="2")
        self.band_blue = tk.StringVar(value="3")
        self.min_display = tk.DoubleVar(value=0.0)
        self.max_display = tk.DoubleVar(value=1.0)
        self.evi_g = tk.DoubleVar(value=2.5)
        self.evi_C1 = tk.DoubleVar(value=6.0)
        self.evi_C2 = tk.DoubleVar(value=7.5)
        self.evi_L = tk.DoubleVar(value=1.0)
        self.current_index = tk.StringVar(value="NDVI")
        
        self.dataset = None
        self.index_image = None
        self.roi_coords = []
        
        # Create the main frame
        main_frame = ttk.Frame(master)
        main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        # Left panel for controls
        control_frame = ttk.LabelFrame(main_frame, text="Controls")
        control_frame.pack(side=tk.LEFT, fill=tk.Y, padx=5, pady=5)
        
        # File selection
        file_frame = ttk.Frame(control_frame)
        file_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(file_frame, text="Satellite Image:").pack(anchor=tk.W)
        
        file_entry = ttk.Entry(file_frame, textvariable=self.filepath, width=30)
        file_entry.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 5))
        
        browse_button = ttk.Button(file_frame, text="Browse", command=self.browse_file)
        browse_button.pack(side=tk.RIGHT)
        
        # Band selection
        band_frame = ttk.LabelFrame(control_frame, text="Bands")
        band_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(band_frame, text="Red Band:").grid(row=0, column=0, sticky=tk.W, padx=5, pady=2)
        ttk.Entry(band_frame, textvariable=self.band_red, width=5).grid(row=0, column=1, sticky=tk.W, padx=5, pady=2)
        
        ttk.Label(band_frame, text="NIR Band:").grid(row=1, column=0, sticky=tk.W, padx=5, pady=2)
        ttk.Entry(band_frame, textvariable=self.band_nir, width=5).grid(row=1, column=1, sticky=tk.W, padx=5, pady=2)
        
        ttk.Label(band_frame, text="Blue Band:").grid(row=2, column=0, sticky=tk.W, padx=5, pady=2)
        ttk.Entry(band_frame, textvariable=self.band_blue, width=5).grid(row=2, column=1, sticky=tk.W, padx=5, pady=2)
        
        # Display range
        display_frame = ttk.LabelFrame(control_frame, text="Display Range")
        display_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(display_frame, text="Min Value:").grid(row=0, column=0, sticky=tk.W, padx=5, pady=2)
        ttk.Entry(display_frame, textvariable=self.min_display, width=5).grid(row=0, column=1, sticky=tk.W, padx=5, pady=2)
        
        ttk.Label(display_frame, text="Max Value:").grid(row=1, column=0, sticky=tk.W, padx=5, pady=2)
        ttk.Entry(display_frame, textvariable=self.max_display, width=5).grid(row=1, column=1, sticky=tk.W, padx=5, pady=2)
        
        # EVI Parameters
        evi_frame = ttk.LabelFrame(control_frame, text="EVI Parameters")
        evi_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Label(evi_frame, text="G:").grid(row=0, column=0, sticky=tk.W, padx=5, pady=2)
        ttk.Entry(evi_frame, textvariable=self.evi_g, width=5).grid(row=0, column=1, sticky=tk.W, padx=5, pady=2)
        
        ttk.Label(evi_frame, text="C1:").grid(row=1, column=0, sticky=tk.W, padx=5, pady=2)
        ttk.Entry(evi_frame, textvariable=self.evi_C1, width=5).grid(row=1, column=1, sticky=tk.W, padx=5, pady=2)
        
        ttk.Label(evi_frame, text="C2:").grid(row=2, column=0, sticky=tk.W, padx=5, pady=2)
        ttk.Entry(evi_frame, textvariable=self.evi_C2, width=5).grid(row=2, column=1, sticky=tk.W, padx=5, pady=2)
        
        ttk.Label(evi_frame, text="L:").grid(row=3, column=0, sticky=tk.W, padx=5, pady=2)
        ttk.Entry(evi_frame, textvariable=self.evi_L, width=5).grid(row=3, column=1, sticky=tk.W, padx=5, pady=2)
        
        # Index selection
        index_frame = ttk.LabelFrame(control_frame, text="Index Selection")
        index_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Radiobutton(index_frame, text="NDVI", variable=self.current_index, value="NDVI").pack(anchor=tk.W, padx=5, pady=2)
        ttk.Radiobutton(index_frame, text="EVI", variable=self.current_index, value="EVI").pack(anchor=tk.W, padx=5, pady=2)
        ttk.Radiobutton(index_frame, text="SAVI", variable=self.current_index, value="SAVI").pack(anchor=tk.W, padx=5, pady=2)
        ttk.Radiobutton(index_frame, text="NDWI", variable=self.current_index, value="NDWI").pack(anchor=tk.W, padx=5, pady=2)
        
        # Calculate button
        calculate_button = ttk.Button(control_frame, text="Calculate Index", command=self.calculate_index)
        calculate_button.pack(fill=tk.X, padx=5, pady=10)
        
        # Analysis tools frame
        analysis_frame = ttk.LabelFrame(control_frame, text="Analysis Tools")
        analysis_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Button(analysis_frame, text="Show Histogram", command=self.show_histogram).pack(fill=tk.X, padx=5, pady=2)
        ttk.Button(analysis_frame, text="Define ROI", command=self.enable_roi_selection).pack(fill=tk.X, padx=5, pady=2)
        ttk.Button(analysis_frame, text="Custom Band Calculator", command=self.open_band_calculator).pack(fill=tk.X, padx=5, pady=2)
        
        # Export options frame
        export_frame = ttk.LabelFrame(control_frame, text="Export Options")
        export_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Button(export_frame, text="Save as GeoTIFF", command=self.save_result).pack(fill=tk.X, padx=5, pady=2)
        ttk.Button(export_frame, text="Export as PNG", command=self.export_as_png).pack(fill=tk.X, padx=5, pady=2)
        
        # Add zoom controls
        zoom_frame = ttk.LabelFrame(control_frame, text="View Controls")
        zoom_frame.pack(fill=tk.X, padx=5, pady=5)
        
        ttk.Button(zoom_frame, text="Enable Zoom", 
                  command=self.enable_zoom_selection).pack(fill=tk.X, padx=5, pady=2)
        ttk.Button(zoom_frame, text="Reset View", 
                  command=self.reset_zoom).pack(fill=tk.X, padx=5, pady=2)
        
        # Right panel for display
        display_frame = ttk.LabelFrame(main_frame, text="Results")
        display_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True, padx=5, pady=5)
        
        # Set up matplotlib figure
        self.fig, (self.ax1, self.ax2) = plt.subplots(1, 2, figsize=(10, 6))
        self.canvas = FigureCanvasTkAgg(self.fig, master=display_frame)
        self.canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)
        
        # Status bar
        self.status_var = tk.StringVar(value="Ready. Please select a satellite image file.")
        status_bar = ttk.Label(master, textvariable=self.status_var, relief=tk.SUNKEN, anchor=tk.W)
        status_bar.pack(side=tk.BOTTOM, fill=tk.X)
    
    def browse_file(self):
        filetypes = (
            ("GeoTIFF files", "*.tif *.tiff"),
            ("All files", "*.*")
        )
        
        filename = filedialog.askopenfilename(
            title="Select a satellite image file",
            filetypes=filetypes
        )
        
        if filename:
            self.filepath.set(filename)
            self.status_var.set(f"File selected: {os.path.basename(filename)}")
            self.load_image()
    
    def load_image(self):
        try:
            self.dataset = rasterio.open(self.filepath.get())
            band_count = self.dataset.count
            
            # Check band availability
            requested_bands = [int(self.band_red.get()), int(self.band_nir.get()), int(self.band_blue.get())]
            max_requested = max(requested_bands)
            if max_requested > band_count:
                messagebox.showerror("Error", f"The selected file has {band_count} bands, but band {max_requested} was requested. Please adjust band settings.")
                self.dataset = None
                return
            
            # Display RGB composite
            self.ax1.clear()
            rgb_bands = [int(self.band_red.get()), 2, int(self.band_blue.get())]  # Default Green to 2
            
            # Adjust if Green band (2) exceeds band count
            if 2 > band_count:
                rgb_bands[1] = 1  # Fallback to Red if Green isn’t available
            
            rgb = np.dstack([self.dataset.read(i) for i in rgb_bands])
            
            # Normalize for display
            rgb_norm = np.zeros_like(rgb, dtype=np.float32)
            for i in range(3):
                band = rgb[:,:,i]
                min_val = np.percentile(band, 2)
                max_val = np.percentile(band, 98)
                rgb_norm[:,:,i] = np.clip((band - min_val) / (max_val - min_val), 0, 1)
            
            self.ax1.imshow(rgb_norm)
            self.ax1.set_title("RGB Composite")
            self.ax1.axis('off')
            
            self.canvas.draw()
            self.status_var.set(f"Loaded {os.path.basename(self.filepath.get())} - {self.dataset.width}x{self.dataset.height}px, {band_count} bands")
        except Exception as e:
            messagebox.showerror("Error", f"Failed to load image: {str(e)}")
            self.status_var.set("Error loading image.")

    def calculate_ndvi(self):
        try:
            red_band = int(self.band_red.get())
            nir_band = int(self.band_nir.get())
            
            # Check if bands exist
            max_band = max(red_band, nir_band)
            if max_band > self.dataset.count:
                messagebox.showerror("Error", f"The selected file only has {self.dataset.count} bands, but band {max_band} was requested.")
                return None
            
            red = self.dataset.read(red_band).astype(np.float32)
            nir = self.dataset.read(nir_band).astype(np.float32)
            
            # Avoid division by zero
            denominator = nir + red
            ndvi = np.where(denominator > 0, (nir - red) / denominator, 0)
            
            return ndvi
        except Exception as e:
            messagebox.showerror("Error", f"Failed to calculate NDVI: {str(e)}")
            return None
    
    def calculate_evi(self):
        try:
            red_band = int(self.band_red.get())
            nir_band = int(self.band_nir.get())
            blue_band = int(self.band_blue.get())
            
            # Check if bands exist
            max_band = max(red_band, nir_band, blue_band)
            if max_band > self.dataset.count:
                messagebox.showerror("Error", f"The selected file only has {self.dataset.count} bands, but band {max_band} was requested.")
                return None
            
            red = self.dataset.read(red_band).astype(np.float32)
            nir = self.dataset.read(nir_band).astype(np.float32)
            blue = self.dataset.read(blue_band).astype(np.float32)
            
            # Get EVI parameters
            G = self.evi_g.get()
            C1 = self.evi_C1.get()
            C2 = self.evi_C2.get()
            L = self.evi_L.get()
            
            # EVI formula: G * (NIR - RED) / (NIR + C1 * RED - C2 * BLUE + L)
            denominator = nir + C1 * red - C2 * blue + L
            evi = np.where(denominator > 0, G * (nir - red) / denominator, 0)
            
            return evi
        except Exception as e:
            messagebox.showerror("Error", f"Failed to calculate EVI: {str(e)}")
            return None

    def calculate_savi(self):
        # SAVI (Soil-Adjusted Vegetation Index)
        red_band = int(self.band_red.get())
        nir_band = int(self.band_nir.get())
        L = 0.5  # Soil brightness correction factor
        
        red = self.dataset.read(red_band).astype(np.float32)
        nir = self.dataset.read(nir_band).astype(np.float32)
        
        return ((nir - red) / (nir + red + L)) * (1 + L)

    def calculate_ndwi(self):
        # NDWI (Normalized Difference Water Index)
        green_band = 2  # Assuming band 2 is green
        nir_band = int(self.band_nir.get())
        
        green = self.dataset.read(green_band).astype(np.float32)
        nir = self.dataset.read(nir_band).astype(np.float32)
        
        return (green - nir) / (green + nir)

    def show_histogram(self):
        if self.index_image is None:
            messagebox.showwarning("Warning", "Calculate an index first.")
            return
            
        # Create a new window for the histogram
        hist_window = tk.Toplevel(self.master)
        hist_window.title(f"{self.current_index.get()} Histogram")
        
        fig, ax = plt.subplots(figsize=(8, 6))
        valid_data = self.index_image[~np.isnan(self.index_image) & ~np.isinf(self.index_image)]
        ax.hist(valid_data.flatten(), bins=100)
        ax.set_title(f"{self.current_index.get()} Distribution")
        ax.set_xlabel("Value")
        ax.set_ylabel("Frequency")
        
        canvas = FigureCanvasTkAgg(fig, master=hist_window)
        canvas.get_tk_widget().pack(fill=tk.BOTH, expand=True)

    def enable_roi_selection(self):
        self.roi_coords = []
        self.canvas.mpl_connect('button_press_event', self.on_click)
        self.status_var.set("Click to define ROI points. Double-click to complete.")
        
    def on_click(self, event):
        if event.dblclick:
            self.complete_roi()
            return
            
        if event.inaxes == self.ax2:
            self.roi_coords.append((event.xdata, event.ydata))
            self.ax2.plot(event.xdata, event.ydata, 'ro')
            self.canvas.draw()
            
    def complete_roi(self):
        if len(self.roi_coords) < 3:
            messagebox.showwarning("Warning", "Need at least 3 points for a valid ROI.")
            return
            
        # Create a polygon from the points
        from matplotlib.path import Path
        roi_path = Path(self.roi_coords)
        
        # Create a mask for the ROI
        y, x = np.mgrid[:self.index_image.shape[0], :self.index_image.shape[1]]
        points = np.vstack((x.ravel(), y.ravel())).T
        mask = roi_path.contains_points(points)
        mask = mask.reshape(self.index_image.shape)
        
        # Calculate statistics for the ROI
        roi_values = self.index_image[mask]
        results = {
            "Mean": np.mean(roi_values),
            "Median": np.median(roi_values),
            "Min": np.min(roi_values),
            "Max": np.max(roi_values),
            "Std Dev": np.std(roi_values)
        }
        
        # Display results
        result_str = "\n".join([f"{k}: {v:.4f}" for k, v in results.items()])
        messagebox.showinfo("ROI Statistics", result_str)

    def open_band_calculator(self):
        calc_window = tk.Toplevel(self.master)
        calc_window.title("Custom Band Calculator")
        calc_window.geometry("400x300")
        
        ttk.Label(calc_window, text="Enter your formula using band1, band2, etc.:").pack(pady=5)
        formula_entry = ttk.Entry(calc_window, width=40)
        formula_entry.pack(pady=5)
        formula_entry.insert(0, "(band5 - band4) / (band5 + band4)")  # Example NDVI
        
        ttk.Label(calc_window, text="Assign bands (comma-separated numbers):").pack(pady=5)
        bands_entry = ttk.Entry(calc_window, width=40)
        bands_entry.pack(pady=5)
        
        def calculate_custom():
            try:
                formula = formula_entry.get()
                bands = [int(b.strip()) for b in bands_entry.get().split(",")]
                
                # Load the bands
                band_data = {}
                for i, band_num in enumerate(bands, 1):
                    band_data[f"band{i}"] = self.dataset.read(band_num).astype(np.float32)
                    
                # Evaluate the formula
                result = eval(formula, {"__builtins__": {}}, band_data)
                
                # Display the result
                self.index_image = result
                self.display_result("Custom Index")
                calc_window.destroy()
            except Exception as e:
                messagebox.showerror("Error", f"Failed to calculate: {str(e)}")
        
        ttk.Button(calc_window, text="Calculate", command=calculate_custom).pack(pady=10)

    def export_as_png(self):
        if self.index_image is None:
            messagebox.showwarning("Warning", "Calculate an index first.")
            return
            
        save_path = filedialog.asksaveasfilename(
            title="Export as PNG",
            defaultextension=".png",
            filetypes=(("PNG files", "*.png"), ("All files", "*.*"))
        )
        
        if save_path:
            try:
                plt.figure(figsize=(10, 8))
                plt.imshow(self.index_image, cmap='RdYlGn', vmin=self.min_display.get(), vmax=self.max_display.get())
                plt.colorbar(label=self.current_index.get())
                plt.title(f"{self.current_index.get()} - {os.path.basename(self.filepath.get())}")
                plt.axis('off')
                plt.tight_layout()
                plt.savefig(save_path, dpi=300)
                plt.close()
                messagebox.showinfo("Success", f"Image exported to {save_path}")
            except Exception as e:
                messagebox.showerror("Error", f"Failed to export: {str(e)}")

    def add_zoom_controls(self):
        from matplotlib.widgets import RectangleSelector
        
        def on_select(eclick, erelease):
            x1, y1 = eclick.xdata, y1 = eclick.ydata
            x2, y2 = erelease.xdata, y2 = erelease.ydata
            
            # Update the displayed region
            self.ax1.set_xlim(min(x1, x2), max(x1, x2))
            self.ax1.set_ylim(min(y1, y2), max(y1, y2))
            self.ax2.set_xlim(min(x1, x2), max(x1, x2))
            self.ax2.set_ylim(min(y1, y2), max(y1, y2))
            self.canvas.draw()
        
        # Add reset zoom button
        zoom_frame = ttk.Frame(self.master)
        zoom_frame.pack(fill=tk.X)
        
        ttk.Button(zoom_frame, text="Enable Zoom Selection", 
                   command=lambda: RectangleSelector(self.ax1, on_select, useblit=True)).pack(side=tk.LEFT)
        
        ttk.Button(zoom_frame, text="Reset Zoom", 
                   command=lambda: [self.ax1.autoscale(), self.ax2.autoscale(), self.canvas.draw()]).pack(side=tk.LEFT)
    

    def calculate_index(self):
        if self.dataset is None:
            messagebox.showwarning("Warning", "Please load an image first.")
            return
        
        index_type = self.current_index.get()
        
        if index_type == "NDVI":
            self.index_image = self.calculate_ndvi()
        elif index_type == "EVI":
            self.index_image = self.calculate_evi()
        elif index_type == "SAVI":
            self.index_image = self.calculate_savi()
        elif index_type == "NDWI":
            self.index_image = self.calculate_ndwi()
        
        if self.index_image is not None:
            self.display_result(index_type)
    
    def display_result(self, index_type):
        min_val = self.min_display.get()
        max_val = self.max_display.get()
        
        # Clear the entire figure and recreate the subplots with proper sizing
        plt.close(self.fig)
        self.fig = plt.figure(figsize=(10, 6))
        self.ax1 = self.fig.add_subplot(1, 2, 1)
        self.ax2 = self.fig.add_subplot(1, 2, 2)
        
        # Redisplay the RGB composite
        if self.dataset is not None:
            rgb_bands = [int(self.band_red.get()), 2, int(self.band_blue.get())]
            
            # Adjust if Green band (2) exceeds band count
            if 2 > self.dataset.count:
                rgb_bands[1] = 1
                
            rgb = np.dstack([self.dataset.read(i) for i in rgb_bands])
            
            # Normalize for display
            rgb_norm = np.zeros_like(rgb, dtype=np.float32)
            for i in range(3):
                band = rgb[:,:,i]
                min_val_rgb = np.percentile(band, 2)
                max_val_rgb = np.percentile(band, 98)
                rgb_norm[:,:,i] = np.clip((band - min_val_rgb) / (max_val_rgb - min_val_rgb), 0, 1)
            
            self.ax1.imshow(rgb_norm)
            self.ax1.set_title("RGB Composite")
            self.ax1.axis('off')
        
        # Create a custom colormap for vegetation indices
        colors = ['#d73027', '#f46d43', '#fdae61', '#fee08b', '#d9ef8b', '#a6d96a', '#66bd63', '#1a9850']
        cmap = LinearSegmentedColormap.from_list('vegetation', colors)
        
        # Display the index image
        im = self.ax2.imshow(self.index_image, cmap=cmap, vmin=min_val, vmax=max_val)
        self.ax2.set_title(f"{index_type} Result")
        self.ax2.axis('off')
        
        # Add colorbar with consistent positioning
        cbar = self.fig.colorbar(im, ax=self.ax2, orientation='vertical', shrink=0.8, pad=0.02)
        cbar.set_label(index_type)
        
        # Apply tight layout to ensure proper spacing
        self.fig.tight_layout()
        
        # Update the canvas with the new figure
        self.canvas.figure = self.fig
        self.canvas.draw()
        
        # Calculate statistics
        valid_pixels = self.index_image[~np.isnan(self.index_image) & ~np.isinf(self.index_image)]
        if len(valid_pixels) > 0:
            min_idx = np.min(valid_pixels)
            max_idx = np.max(valid_pixels)
            mean_idx = np.mean(valid_pixels)
            median_idx = np.median(valid_pixels)
            self.status_var.set(f"{index_type} calculated. Min: {min_idx:.3f}, Max: {max_idx:.3f}, Mean: {mean_idx:.3f}, Median: {median_idx:.3f}")
        else:
            self.status_var.set(f"{index_type} calculated but contains no valid pixels.")
    
    def save_result(self):
        if self.index_image is None:
            messagebox.showwarning("Warning", "Please calculate an index first.")
            return
        
        # Ask for save location
        try:
            save_path = filedialog.asksaveasfilename(
                title="Save Result",
                defaultextension=".tif",
                filetypes=(("GeoTIFF files", "*.tif"), ("All files", "*.*"))
            )
            if not save_path:
                return
            
            # Save the result as GeoTIFF
            self.status_var.set(f"Result saved to {os.path.basename(save_path)}")
            messagebox.showinfo("Success", f"Result saved successfully to:\n{save_path}")
        except Exception as e:
            messagebox.showerror("Error", f"Failed to save result: {str(e)}")

    def enable_zoom_selection(self):
        from matplotlib.widgets import RectangleSelector

        def on_select(eclick, erelease):
            x1, y1 = eclick.xdata, y1 = eclick.ydata
            x2, y2 = erelease.xdata, y2 = erelease.ydata

            # Update the displayed region
            self.ax1.set_xlim(min(x1, x2), max(x1, x2))
            self.ax1.set_ylim(min(y1, y2), max(y1, y2))
            self.ax2.set_xlim(min(x1, x2), max(x1, x2))
            self.ax2.set_ylim(min(y1, y2), max(y1, y2))
            self.canvas.draw()

        self.rect_selector = RectangleSelector(
            self.ax2,
            on_select,
            useblit=True,
            button=[1],
            minspanx=5,
            minspany=5,
            spancoords='pixels',
            interactive=True
        )
        self.status_var.set("Drag to select an area to zoom")

    def reset_zoom(self):
        # Reset both axes to their default view
        self.ax1.autoscale()
        self.ax2.autoscale()
        self.canvas.draw()
        self.status_var.set("View reset to default zoom level")


def main():
    root = tk.Tk()
    app = VegetationIndicesCalculator(root)
    root.mainloop()


if __name__ == "__main__":
    main()