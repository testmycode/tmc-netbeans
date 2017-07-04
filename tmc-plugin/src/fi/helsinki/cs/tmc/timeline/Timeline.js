/* global SVG */

var timeline = (function () {
    var canvas = SVG('svgCanvas').size(1000, 100);
    var baseline = 50;
    var margin = 50;
    var ballSize = 20;
    var rowLength = 10;

    var ball = function (x, y, name, color) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.body = canvas.circle(ballSize).move(this.x, this.y).fill({color: color});
        this.text = "";
        this.body.mouseover(function () {
            this.text = canvas.text(name).move(0, 0);
        });
        this.body.mouseout(function () {
            this.text.clear();
        });
    };

    var heart = function (x, y, name) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.body = [canvas.circle(15).move(this.x, this.y), 
            canvas.circle(15).move(this.x + 10, this.y), 
            canvas.rect(15, 15).move(this.x + 5, this.y+5).rotate(45)];
        this.text = "";
        this.body[0].mouseover(function () {
            this.text = canvas.text(name).move(0, 0);
        });
        this.body[1].mouseover(function () {
            this.text = canvas.text(name).move(0, 0);
        });
        this.body[2].mouseover(function () {
            this.text = canvas.text(name).move(0, 0);
        });
        this.body[0].mouseout(function () {
            this.text.clear();
        });
        this.body[1].mouseout(function () {
            this.text.clear();
        });
        this.body[2].mouseout(function () {
            this.text.clear();
        });
        this.fillHeart = function (fillColor) {
            this.body[0].fill({color: fillColor});
            this.body[1].fill({color: fillColor});
            this.body[2].fill({color: fillColor});
        };
    };

    var setCanvasSize = function (elementCount) {
        var rowCount = parseInt(elementCount / rowLength) + 1;
        if(elementCount < 10) {
            canvas.size(margin * elementCount + ballSize, baseline + 2 * margin);
        } else {
            canvas.size(margin * rowLength + ballSize, baseline + (rowCount + 1) * margin);
        }
    };

    var drawHearts = function (hearts, exerciseCount) {
        var i = 0;
        for (var skillName in hearts) {
            var currElementCount = exerciseCount + i;
            var x = currElementCount % rowLength === 0 ? 0 : currElementCount % rowLength * margin ;
            var y = currElementCount % rowLength === 0 ? baseline + (parseInt(exerciseCount/rowLength) + 1) * margin
                    : baseline + (parseInt(exerciseCount/rowLength)) * margin;
            var newHeart = new heart(x, y, skillName);
            var color = hearts[skillName];
            newHeart.fillHeart(color);
            i++;
        }
    };

    var drawBalls = function (exerciseMap) {
        var i = 0;
        for (var name in exerciseMap) {
            var x = margin * (i % rowLength);
            var y = baseline + margin * parseInt(i / rowLength);
            var color = exerciseMap[name];
            new ball(x, y, name, color);
            i++;
        }
    };
    
    var drawHorizontalLines = function(elementCount) {
        var horizontalLinesCount = parseInt(elementCount / rowLength) + 1;
        var elementsLeft = elementCount;
        var ballRadius = ballSize / 2;
        
        for (var i = 0; i < horizontalLinesCount; i++) {
            var elements = elementsLeft >= rowLength ? 10 : elementsLeft % 10;
            var x1 = 1;
            var y1 = i * margin + baseline + ballRadius;
            var x2 = (elements - 1) * margin + 1;
            var y2 = y1;
            canvas.line(x1, y1, x2, y2).stroke({width: 3});
            
            elementsLeft -= elements;
        }
    };
    
    var drawVerticalLines = function(elementCount) {
        var verticalLinesCount = parseInt(elementCount / rowLength);
        var ballRadius = ballSize / 2;
        
        for (var i = 0; i < verticalLinesCount; i++) {
            var x1 = (rowLength - 1) * margin + ballRadius;
            var y1 = i * margin + baseline + ballRadius;
            var x2 = (rowLength - 1) * margin + ballRadius;
            var y2 = i * margin + baseline + ballRadius + 15;
            canvas.line(x1, y1, x2, y2).stroke({width: 1});
            
            var x1 = ballRadius;
            var y1 = (i+1) * margin + baseline + ballRadius;
            var x2 = ballRadius;
            var y2 = (i+1) * margin - 15 + baseline + ballRadius;
            canvas.line(x1, y1, x2, y2).stroke({width: 1});
        }
    };
    
    var drawDiagonalLines = function(elementCount) {
        var diagonalLinesCount = parseInt(elementCount / rowLength);
        var ballRadius = ballSize / 2;
        
        for (var i = 0; i < diagonalLinesCount; i++) {
            var x1 = ballRadius;
            var y1 = (i+1) * margin - 15 + baseline + ballRadius;
            var x2 = (rowLength - 1) * margin + ballRadius;
            var y2 = i * margin + baseline + ballRadius + 15;
            canvas.line(x1, y1, x2, y2).stroke({width: 1});
        }
    };

    var drawLines = function (elementCount) {
        drawHorizontalLines(elementCount);
        drawVerticalLines(elementCount);
        drawDiagonalLines(elementCount);
    };

    return {
        setCanvasSize: setCanvasSize,
        drawBalls: drawBalls,
        drawHearts: drawHearts,
        drawLines: drawLines
    };
})();

var timelineWrapper = (function () {    
    var drawTimeline = function (exerciseJsonMap, skillJsonMap) {
        var exercises = JSON.parse(exerciseJsonMap);
        var skills = JSON.parse(skillJsonMap);
        
        var exerciseCount = Object.keys(exercises).length;
        var skillCount = Object.keys(skills).length;
        var elementCount = exerciseCount + skillCount;
        
        timeline.setCanvasSize(elementCount);
        timeline.drawLines(elementCount);
        timeline.drawBalls(exercises);
        timeline.drawHearts(skills, exerciseCount);
    };

    return {
        drawTimeline: drawTimeline
    };
})();
