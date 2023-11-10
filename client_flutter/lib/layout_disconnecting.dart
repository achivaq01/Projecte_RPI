import 'package:flutter/cupertino.dart';

class LayoutDisconnecting extends StatelessWidget {
  const LayoutDisconnecting({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(
        middle: Text("WebSockets Client"),
      ),
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: const [
          SizedBox(height: 75),
          Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(
                "Disconnecting...",
                style: TextStyle(
                  fontSize: 14,
                  color: Color.fromRGBO(200, 0, 0, 1),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
