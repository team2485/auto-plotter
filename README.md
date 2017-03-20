# auto-plotter
This program is used to create paths that can be followed by the robot autonomously using code from frc-2017. To use this program, a few things must be changed to match your team. 
1. Replace the file Resources/robot.png with a render of your robot. It must be scaled appropriately, such that 2 pixels in the image corresponds to 1 inch on the robot (to do this make a drawing file of your robot at 1:50 scaling, crop the drawing file and save it as a ping, and finally set the resolution to 100 PPI).
2. If this is not being used with the 2017 frc game, steamworks, you must replace fieldR.png and fieldB.png with field drawings for each side.
3. Run the program through eclipse, or export as a jar and run. 

Now, you can use the help menu to see the control, and you can use the file menu to save and open autos. When the files are saved, they can be opened as text and copied directly into robot code. (omit the initial "R" or "B" which represent which side of the field the auto is on, as well as the next 2 numbers, which represent the starting location in pixels).